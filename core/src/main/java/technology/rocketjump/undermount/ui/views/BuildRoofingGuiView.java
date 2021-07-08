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
import technology.rocketjump.undermount.ui.actions.SetInteractionMode;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.LinkedList;
import java.util.List;

@Singleton
public class BuildRoofingGuiView implements GuiView/*, I18nUpdatable */{

	private final ItemTracker itemTracker;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;


	private final List<IconButton> iconButtons = new LinkedList<>();

	@Inject
	public BuildRoofingGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
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
		IconButton addRoofing = iconButtonFactory.create("GUI.ROOFING.ADD", "triple-gate", HexColors.POSITIVE_COLOR, ButtonStyle.DEFAULT);
		addRoofing.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_ROOFING, messageDispatcher));
		iconButtons.add(addRoofing);

		// cancel roofing
		IconButton cancelRoofing = iconButtonFactory.create("GUI.CANCEL_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.DEFAULT);
		cancelRoofing.setAction(new SetInteractionMode(GameInteractionMode.CANCEL_ROOFING, messageDispatcher));
		iconButtons.add(cancelRoofing);

		// deconstruct roofing
		IconButton deconstructRoofing = iconButtonFactory.create("GUI.DECONSTRUCT_LABEL", "demolish", HexColors.get("#d1752e"), ButtonStyle.DEFAULT);
		deconstructRoofing.setAction(new SetInteractionMode(GameInteractionMode.DECONSTRUCT_ROOFING, messageDispatcher));
		iconButtons.add(deconstructRoofing);

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BUILD_ROOFING;
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

//	@Override
//	public void onLanguageUpdated() {
//		resetMaterialTypeSelect(); // For different material names
//	}
//
//	private void resetMaterialTypeSelect() {
//		Array<GameMaterialType> itemsArray = new Array<>();
//		for (GameMaterialType gameMaterialType : resourceTypeMap.keySet()) {
//			itemsArray.add(gameMaterialType);
//		}
//		materialTypeSelect.setItems(itemsArray);
//		materialTypeSelect.setSelected(itemsArray.get(0));
//		resetMaterialSelect();
//	}
//
//	private void resetMaterialSelect() {
//		currentMaterialNamesMap.clear();
//		Array<String> materialTypes = new Array<>();
//
//		String anyString = i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY").toString();
//		currentMaterialNamesMap.put(anyString, NULL_MATERIAL);
//		materialTypes.add(anyString);
//
//		ItemType itemTypeForMaterialType = resourceTypeMap.get(selectedMaterialType);
//
//		if (itemTypeForMaterialType != null) {
//			Set<GameMaterial> materialsByItemType = itemTracker.getMaterialsByItemType(itemTypeForMaterialType);
//			if (materialsByItemType != null) {
//				for (GameMaterial gameMaterial : materialsByItemType) {
//					materialTypes.add(gameMaterial.getMaterialName());
//					currentMaterialNamesMap.put(gameMaterial.getMaterialName(), gameMaterial);
//				}
//			}
//		}
//
//		materialSelect.setItems(materialTypes);
//		if (materialTypes.size > 0) {
//			materialSelect.setSelected(materialTypes.get(0));
//			selectedMaterial = currentMaterialNamesMap.get(materialTypes.get(0));
//		}
//		onMaterialSelectionChange();
//	}
//
//	private void onMaterialSelectionChange() {
//		selectedMaterial = currentMaterialNamesMap.get(materialSelect.getSelected());
//		if (initialised) {
//			messageDispatcher.dispatchMessage(MessageType.WALL_MATERIAL_SELECTED, new MaterialSelectionMessage(
//					selectedMaterialType, selectedMaterial, resourceTypeMap.get(selectedMaterialType)));
//		}
//	}

	@Override
	public void onClose() {
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
	}
}
