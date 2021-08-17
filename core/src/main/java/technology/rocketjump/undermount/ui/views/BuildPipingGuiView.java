package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.GameViewMode;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.LinkedList;
import java.util.List;

@Singleton
public class BuildPipingGuiView implements GuiView {

	private final ItemTracker itemTracker;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;


	private final List<IconButton> iconButtons = new LinkedList<>();

	@Inject
	public BuildPipingGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							  IconButtonFactory iconButtonFactory, ItemTracker itemTracker,
							  I18nTranslator i18nTranslator, I18nWidgetFactory i18NWidgetFactory) {
		this.messageDispatcher = messageDispatcher;
		this.itemTracker = itemTracker;
		this.i18nTranslator = i18nTranslator;

		Skin uiSkin = guiSkinRepository.getDefault();

		IconButton back = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
		back.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.BUILD_MENU);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
		});
		iconButtons.add(back);

		// add roofing
		IconButton addRoofing = iconButtonFactory.create("GUI.ROOFING.ADD", "pipes", HexColors.POSITIVE_COLOR, ButtonStyle.DEFAULT);
//		addRoofing.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_PIPING, messageDispatcher));
		iconButtons.add(addRoofing);

		// cancel roofing
		IconButton cancelRoofing = iconButtonFactory.create("GUI.CANCEL_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.DEFAULT);
//		cancelRoofing.setAction(new SetInteractionMode(GameInteractionMode.CANCEL_PIPING, messageDispatcher));
		iconButtons.add(cancelRoofing);

		// deconstruct roofing
		IconButton deconstructRoofing = iconButtonFactory.create("GUI.DECONSTRUCT_LABEL", "demolish", HexColors.get("#d1752e"), ButtonStyle.DEFAULT);
//		deconstructRoofing.setAction(new SetInteractionMode(GameInteractionMode.DECONSTRUCT_PIPING, messageDispatcher));
		iconButtons.add(deconstructRoofing);

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BUILD_PIPING;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.BUILD_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		for (IconButton iconButton : iconButtons) {
			containerTable.add(iconButton).pad(5);
		}
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

	@Override
	public void onClose() {
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
	}
}
