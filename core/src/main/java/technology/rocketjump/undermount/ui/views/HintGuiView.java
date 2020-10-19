package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisProgressBar;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.ui.hints.HintDictionary;
import technology.rocketjump.undermount.ui.hints.HintProgressEvaluator;
import technology.rocketjump.undermount.ui.hints.model.*;
import technology.rocketjump.undermount.ui.i18n.I18nString;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nTextButton;
import technology.rocketjump.undermount.ui.widgets.I18nTextWidget;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.ui.hints.model.HintAction.HintActionType.DISMISS;

@Singleton
public class HintGuiView implements GuiView, GameContextAware {

	private static final float UPDATE_PERIOD = 1.23f;
	private static final HintAction DISMISS_ACTION = new HintAction();
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final HintDictionary hintDictionary;
	private final HintProgressEvaluator hintProgressEvaluator;
	private final Skin uiSkin;
	private Table layoutTable;
	private GameContext gameContext;

	private List<Hint> displayedHints = new ArrayList<>();
	private List<HintProgress> currentProgress = new ArrayList<>();

	private float timeSinceLastUpdate = 0f;

	static {
		DISMISS_ACTION.setButtonTextI18nKey("HINT.BUTTON.DISMISS");
		DISMISS_ACTION.setType(DISMISS);
	}

	@Inject
	public HintGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
					   I18nTranslator i18nTranslator,
					   I18nWidgetFactory i18nWidgetFactory, HintDictionary hintDictionary,
					   HintProgressEvaluator hintProgressEvaluator) {
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;
		this.uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;
		this.hintDictionary = hintDictionary;
		this.hintProgressEvaluator = hintProgressEvaluator;

		layoutTable = new Table(uiSkin);
	}


	@Override
	public void populate(Table containerTable) {
		update();
		containerTable.add(this.layoutTable);
	}

	@Override
	public void update() {
		if (gameContext != null) {

			timeSinceLastUpdate += Gdx.graphics.getDeltaTime();
			if (timeSinceLastUpdate > UPDATE_PERIOD) {
				timeSinceLastUpdate = 0f;
				checkForUpdate();
			}
		}
	}

	private void checkForUpdate() {
		List<HintProgress> newProgress = new ArrayList<>();
		for (Hint displayedHint : displayedHints) {
			for (HintProgressDescriptor descriptor : displayedHint.getProgressDescriptors()) {
				newProgress.add(hintProgressEvaluator.evaluate(descriptor));
			}
		}

		if (!newProgress.equals(currentProgress)) {
			doUpdate();
		}
	}

	private void doUpdate() {
		currentProgress.clear();

		layoutTable.clearChildren();

		for (Hint displayedHint : displayedHints) {
			Table hintTable = new Table(uiSkin);
			hintTable.setBackground("default-rect");

			for (String i18nKey : displayedHint.getI18nKeys()) {
				I18nText text = i18nTranslator.getTranslatedString(i18nKey).breakAfterLength(i18nTranslator.getCurrentLanguageType().getBreakAfterLineLength());
				I18nTextWidget widget = new I18nTextWidget(text, uiSkin, messageDispatcher);
				hintTable.add(widget).left().pad(5).row();
			}

			if (!displayedHint.getProgressDescriptors().isEmpty()) {
				hintTable.add(i18nWidgetFactory.createLabel("HINT.PROGRESS.HEADER")).left().pad(5).row();
				try {
					buildProgressDescriptors(displayedHint, hintTable);
				} catch (HintProgressComplete hintProgressComplete) {
					if (displayedHint.getActions().size() == 1) {
						messageDispatcher.dispatchMessage(MessageType.HINT_ACTION_TRIGGERED, displayedHint.getActions().get(0));
					} else {
						Logger.error("Only expecting one action for hint with progress indicators", displayedHint);
					}
					doUpdate();
					break;
				}
			}

			buildActions(displayedHint, hintTable);

			layoutTable.add(hintTable).row();
		}
	}

	public void add(Hint hint) {
		displayedHints.add(hint);
		gameContext.getSettlementState().getCurrentHints().add(hint.getHintId());
		doUpdate();
	}

	public void dismissAll() {
		for (Hint hint : new ArrayList<>(displayedHints)) {
			remove(hint);
		}
	}

	private void remove(Hint hint) {
		displayedHints.remove(hint);
		gameContext.getSettlementState().getCurrentHints().remove(hint.getHintId());
		gameContext.getSettlementState().getPreviousHints().put(hint.getHintId(), true);
		doUpdate();
	}

	public List<Hint> getDisplayedHints() {
		return displayedHints;
	}

	@Override
	public GuiViewName getName() {
		// This is a special case GuiView which lives outside of the normal usage
		return null;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		for (String currentHintId : gameContext.getSettlementState().getCurrentHints()) {
			this.displayedHints.add(hintDictionary.getById(currentHintId));
		}
	}

	@Override
	public void clearContextRelatedState() {
		displayedHints.clear();
	}

	private void buildProgressDescriptors(Hint hint, Table hintTable) throws HintProgressComplete {
		boolean allProgressComplete = true;
		for (HintProgressDescriptor progressDescriptor : hint.getProgressDescriptors()) {
			Table progressTable = new Table(uiSkin);

			HintProgress progress = hintProgressEvaluator.evaluate(progressDescriptor);
			currentProgress.add(progress);
			if (!progress.isComplete()) {
				allProgressComplete = false;
			}

			VisProgressBar bar = new VisProgressBar(0, progress.total, 1, false);
			bar.setValue(progress.current);
			progressTable.add(bar).padRight(5);

			Map<String, I18nString> replacements = new HashMap<>();
			replacements.put("currentQuantity", new I18nText(String.valueOf(progress.current)));
			replacements.put("requiredQuantity", new I18nText(String.valueOf(progress.total)));
			replacements.put("targetDescription", progress.targetDescription);
			I18nText text = i18nTranslator.getTranslatedWordWithReplacements("TUTORIAL.PROGRESS_DESCRIPTION.TEXT", replacements);

			progressTable.add(new I18nTextWidget(text, uiSkin, messageDispatcher));

			hintTable.add(progressTable).left().pad(5).row();
		}

		if (allProgressComplete) {
			throw new HintProgressComplete();
		}
	}

	private void buildActions(Hint displayedHint, Table hintTable) {
		if (!displayedHint.getProgressDescriptors().isEmpty()) {
			// Don't show any actions when progress is displayed
			return;
		}
		Table actionsTable = new Table(uiSkin);

		for (HintAction action : displayedHint.getActions()) {
			I18nTextButton button = i18nWidgetFactory.createTextButton(action.getButtonTextI18nKey());
			button.addListener(new ClickListener() {

				boolean triggeredOnce = false;

				@Override
				public void clicked (InputEvent event, float x, float y) {
					if (!triggeredOnce) {
						triggeredOnce = true;
						messageDispatcher.dispatchMessage(MessageType.HINT_ACTION_TRIGGERED, action);
					}
				}
			});
			actionsTable.add(button).left().padRight(5);
		}


		if (displayedHint.isDismissable()) {
			I18nTextButton button = i18nWidgetFactory.createTextButton("HINT.BUTTON.DISMISS");
			button.addListener(new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.HINT_ACTION_TRIGGERED, DISMISS_ACTION);
				}
			});
			actionsTable.add(button).left().padRight(5);
		}

		hintTable.add(actionsTable).left().pad(5).row();
	}
}
