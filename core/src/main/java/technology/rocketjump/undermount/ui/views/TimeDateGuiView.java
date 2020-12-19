package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.screens.GameScreenDictionary;
import technology.rocketjump.undermount.screens.ManagementScreen;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.*;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class TimeDateGuiView implements GuiView, GameContextAware {

	private final I18nTranslator i18nTranslator;
	private final I18nTextWidget dateTimeLabel;
	private final MessageDispatcher messageDispatcher;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final Label settlementNameLabel;
	private Table layoutTable;
	private Table timeDateTable;
	private Table managementScreenButtonTable;
	private GameContext gameContext;

	private List<IconOnlyButton> speedButtons = new ArrayList<>();

	@Inject
	public TimeDateGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						   I18nTranslator i18nTranslator,
						   IconButtonFactory iconButtonFactory, I18nWidgetFactory i18nWidgetFactory) {
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;
		Skin uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;

		timeDateTable = new Table(uiSkin);
		timeDateTable.background("default-rect");
		timeDateTable.pad(5);

		managementScreenButtonTable = new Table(uiSkin);

		for (GameSpeed gameSpeed : GameSpeed.VISIBLE_TO_UI) {
			IconOnlyButton button = iconButtonFactory.create(gameSpeed);
			button.setAction(() -> messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, gameSpeed));
			speedButtons.add(button);
		}

		settlementNameLabel = new Label("", uiSkin);
		timeDateTable.add(settlementNameLabel).colspan(speedButtons.size()).center().padBottom(4).row();

		for (IconOnlyButton speedButton : speedButtons) {
			timeDateTable.add(speedButton);
		}
		timeDateTable.row();

		this.dateTimeLabel = new I18nTextWidget(new I18nText(""), uiSkin, messageDispatcher);
		timeDateTable.add(dateTimeLabel).colspan(speedButtons.size()).center().padTop(4);

		layoutTable = new Table(uiSkin);
		layoutTable.add(managementScreenButtonTable).right().top();
		layoutTable.add(timeDateTable).center();
	}


	public void init(GameScreenDictionary gameScreenDictionary) {
		managementScreenButtonTable.clearChildren();

		for (ManagementScreen managementScreen : gameScreenDictionary.getAllManagementScreens()) {
			I18nTextButton screenButton = i18nWidgetFactory.createTextButton(managementScreen.getTitleI18nKey());
			screenButton.addListener(new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, managementScreen.getName());
				}
			});
			managementScreenButtonTable.add(screenButton).pad(2);
		}
	}

	@Override
	public void populate(Table containerTable) {
		update();
		containerTable.add(this.layoutTable);
	}

	@Override
	public void update() {
		if (gameContext != null) {
			dateTimeLabel.setI18nText(i18nTranslator.getDateTimeString(gameContext.getGameClock()));
		}
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
		if (gameContext != null) {
			settlementNameLabel.setText(gameContext.getSettlementState().getSettlementName());
		}
		if (gameContext.getGameClock().isPaused()) {
			for (IconOnlyButton speedButton : speedButtons) {
				boolean highlight = speedButton.gameSpeed.equals(GameSpeed.PAUSED);
				speedButton.setHighlighted(highlight);
			}
		} else {
			for (IconOnlyButton speedButton : speedButtons) {
				boolean highlight = speedButton.gameSpeed.equals(gameContext.getGameClock().getCurrentGameSpeed());
				speedButton.setHighlighted(highlight);
			}
		}
	}

	@Override
	public void clearContextRelatedState() {

	}

}
