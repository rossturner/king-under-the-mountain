package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;
import technology.rocketjump.undermount.ui.GameViewMode;
import technology.rocketjump.undermount.ui.actions.SetInteractionMode;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.LinkedList;
import java.util.List;

import static technology.rocketjump.undermount.ui.GameInteractionMode.DESIGNATE_MECHANISMS;

@Singleton
public class BuildMechanismsGuiView implements GuiView {

	private final ItemTracker itemTracker;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final GameInteractionStateContainer interactionStateContainer;


	private final List<IconButton> iconButtons = new LinkedList<>();

	@Inject
	public BuildMechanismsGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
								  IconButtonFactory iconButtonFactory, ItemTracker itemTracker,
								  I18nTranslator i18nTranslator, I18nWidgetFactory i18NWidgetFactory,
								  GameInteractionStateContainer interactionStateContainer, MechanismTypeDictionary mechanismTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.itemTracker = itemTracker;
		this.i18nTranslator = i18nTranslator;
		this.interactionStateContainer = interactionStateContainer;

		MechanismType gearMechanismType = mechanismTypeDictionary.getByName("Gear");
		MechanismType horizontalShaftMechanismType = mechanismTypeDictionary.getByName("Shaft_Horizontal");
		MechanismType verticalShaftMechanismType = mechanismTypeDictionary.getByName("Shaft_Vertical");

		Skin uiSkin = guiSkinRepository.getDefault();

		IconButton back = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
		back.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.BUILD_MENU);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
		});
		iconButtons.add(back);

		IconButton addGears = iconButtonFactory.create("PRODUCT.STONE.GEAR", "gears", HexColors.get("#DDDDDE"), ButtonStyle.DEFAULT);
		addGears.setAction(() -> {
			interactionStateContainer.setMechanismTypeToPlace(gearMechanismType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_MECHANISMS);
		});
		iconButtons.add(addGears);

		IconButton addHorizontalShafts = iconButtonFactory.create("GUI.MECHANISM.SHAFT.HORIZONTAL", "straight-line-horiz", HexColors.get("#a1511b"), ButtonStyle.DEFAULT);
		addHorizontalShafts.setAction(() -> {
			interactionStateContainer.setMechanismTypeToPlace(horizontalShaftMechanismType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_MECHANISMS);
		});
		iconButtons.add(addHorizontalShafts);

		IconButton addVerticalShafts = iconButtonFactory.create("GUI.MECHANISM.SHAFT.VERTICAL", "straight-line", HexColors.get("#a1511b"), ButtonStyle.DEFAULT);
		addVerticalShafts.setAction(() -> {
			interactionStateContainer.setMechanismTypeToPlace(verticalShaftMechanismType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_MECHANISMS);
		});
		iconButtons.add(addVerticalShafts);

		IconButton cancelRoofing = iconButtonFactory.create("GUI.CANCEL_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.DEFAULT);
		cancelRoofing.setAction(new SetInteractionMode(GameInteractionMode.CANCEL_MECHANISMS, messageDispatcher));
		iconButtons.add(cancelRoofing);

		IconButton deconstructRoofing = iconButtonFactory.create("GUI.DECONSTRUCT_LABEL", "demolish", HexColors.get("#d1752e"), ButtonStyle.DEFAULT);
		deconstructRoofing.setAction(new SetInteractionMode(GameInteractionMode.DECONSTRUCT_MECHANISMS, messageDispatcher));
		iconButtons.add(deconstructRoofing);
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BUILD_MECHANISMS;
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
