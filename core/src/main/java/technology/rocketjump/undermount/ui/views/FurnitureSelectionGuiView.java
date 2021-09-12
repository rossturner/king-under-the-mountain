package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.actions.FurnitureSelectedAction;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.List;
import java.util.*;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.undermount.ui.widgets.ButtonStyle.LARGE;

@Singleton
public class FurnitureSelectionGuiView implements GuiView, FurnitureSelectedCallback, I18nUpdatable {

	private final int ITEMS_PER_ROW = 4;
	private final IconButtonFactory iconButtonFactory;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final ItemTracker itemTracker;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final Table furnitureTable;

	// I18nUpdatable
	private final Label headingLabel;
	private final Label typeLabel;
	private final TextButton backButton;
	private final TextButton nextButton;

	private Table viewTable;

	private SelectBox<GameMaterialType> materialTypeSelect;
	private final SelectBox<String> materialSelect;

	private FurnitureType selectedFurnitureType;
	private GameMaterialType selectedMaterialType = GameMaterialType.WOOD;
	private GameMaterial selectedMaterial;

	private Map<GameMaterialType, ItemType> resourceTypeMap = new TreeMap<>();
	private final Map<String, GameMaterial> currentMaterialNamesMap = new HashMap<>();
	private RoomType currentRoomType;
	private List<FurnitureType> furnitureTypesForRoom = new LinkedList();

	private boolean initialised = false;

	@Inject
	public FurnitureSelectionGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
									 IconButtonFactory iconButtonFactory, FurnitureTypeDictionary furnitureTypeDictionary,
									 ItemTracker itemTracker, I18nTranslator i18nTranslator, I18nWidgetFactory i18NWidgetFactory) {
		this.messageDispatcher = messageDispatcher;
		this.iconButtonFactory = iconButtonFactory;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.itemTracker = itemTracker;
		this.i18nTranslator = i18nTranslator;


		Skin uiSkin = guiSkinRepository.getDefault();

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");
//		viewTable.setDebug(true);


		headingLabel = i18NWidgetFactory.createLabel("GUI.BUILD_FURNITURE");
		viewTable.add(headingLabel).center().colspan(3);
		viewTable.row();

		materialTypeSelect = new SelectBox<>(uiSkin);
		materialTypeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				selectedMaterialType = materialTypeSelect.getSelected();
				resetMaterialSelect();
			}
		});

		this.typeLabel = i18NWidgetFactory.createLabel("MATERIAL_TYPE");
		viewTable.add(typeLabel).right();
		viewTable.add(materialTypeSelect).left();

		materialSelect = new SelectBox<>(uiSkin);
		materialSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				onMaterialSelectionChange();
			}
		});

		viewTable.add(materialSelect).left();
		viewTable.row();


		furnitureTable = new Table(uiSkin);
		furnitureTable.setWidth(400);
		furnitureTable.setHeight(300);
		resetFurnitureTable();
		resetMaterialTypeSelect();


		ScrollPane scrollPane = new ScrollPane(furnitureTable, uiSkin);
		scrollPane.setScrollingDisabled(true, false);
		scrollPane.setForceScroll(false, true);
		ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle(uiSkin.get(ScrollPane.ScrollPaneStyle.class));
		scrollPaneStyle.background = null;
		scrollPane.setStyle(scrollPaneStyle);
		scrollPane.setFadeScrollBars(false);

		viewTable.add(scrollPane).colspan(3);//.height(400);
		viewTable.row();

		backButton = i18NWidgetFactory.createTextButton("GUI.BACK_LABEL");
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ROOM_SIZING);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
			}
		});
		viewTable.add(backButton).pad(10).left().colspan(2);


		nextButton = i18NWidgetFactory.createTextButton("GUI.NEXT_LABEL");
		nextButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.DEFAULT_MENU);
			}
		});
		viewTable.add(nextButton).pad(10).right().colspan(1);

		onLanguageUpdated();

		initialised = true;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ROOM_FURNITURE_SELECTION;
	}

	@Override
	public GuiViewName getParentViewName() {
		if (currentRoomType == null) {
			return GuiViewName.BUILD_MENU;
		} else {
			return GuiViewName.ROOM_SIZING;
		}
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();

		resetMaterialSelect();
		resetFurnitureTable();

		containerTable.add(viewTable);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

	@Override
	public void furnitureTypeSelected(FurnitureType furnitureType) {
		this.selectedFurnitureType = furnitureType;
		resetMaterialTypeSelect();
	}

	private void onMaterialSelectionChange() {
		selectedMaterial = currentMaterialNamesMap.get(materialSelect.getSelected());
		if (initialised) {
			messageDispatcher.dispatchMessage(MessageType.FURNITURE_MATERIAL_SELECTED);
		}
	}

	private void resetFurnitureTable() {
		furnitureTable.clearChildren();

		int numRoomsAdded = 0;
		furnitureTypesForRoom.clear();

		resourceTypeMap.clear();

		// First add room-specific furniture
		if (currentRoomType != null) {
			for (String furnitureName : currentRoomType.getFurnitureNames()) {
				FurnitureType furnitureType = furnitureTypeDictionary.getByName(furnitureName);
				if (furnitureType == null) {
					Logger.error("Could not find furniture type by name " + furnitureName + " for room " + currentRoomType);
				} else {
					if (!furnitureType.isHiddenFromPlacementMenu()) {
						furnitureTypesForRoom.add(furnitureType);
						for (Map.Entry<GameMaterialType, List<QuantifiedItemType>> entry : furnitureType.getRequirements().entrySet()) {
							if (!entry.getValue().isEmpty()) {
								resourceTypeMap.put(entry.getKey(), entry.getValue().get(0).getItemType()); // FIXME Assuming furniture built from single resource type
							}
						}
					}

				}
			}
		} else {
			// Place-anywhere furniture only in non-room furniture collection
			furnitureTypesForRoom.addAll(furnitureTypeDictionary.getPlaceAnywhereFurniture());
		}


		for (FurnitureType furnitureType : furnitureTypesForRoom) {
			if (furnitureType.getFurnitureCategory() == null) {
				continue;
			}

			IconButton iconButton = iconButtonFactory.create(furnitureType.getI18nKey(), furnitureType.getIconName(), furnitureType.getColor(), LARGE);
			iconButton.setAction(new FurnitureSelectedAction(furnitureType, messageDispatcher, this));
			furnitureTable.add(iconButton).pad(10);
			numRoomsAdded++;

			if (numRoomsAdded % ITEMS_PER_ROW == 0) {
				furnitureTable.row();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void resetMaterialTypeSelect() {
		Array<GameMaterialType> itemsArray = new Array<>();
		if (selectedFurnitureType == null) {
			itemsArray.add(GameMaterialType.STONE);
			itemsArray.add(GameMaterialType.WOOD);
			itemsArray.add(GameMaterialType.METAL);
		} else {
			Map<GameMaterialType, ItemType> resourceTypeMap = new TreeMap<>();
			for (Map.Entry<GameMaterialType, List<QuantifiedItemType>> entry : selectedFurnitureType.getRequirements().entrySet()) {
				itemsArray.add(entry.getKey());
				for (QuantifiedItemType requirement : entry.getValue()) {
					if (requirement.getItemType().getPrimaryMaterialType().equals(entry.getKey())) {
						// Note that this will deterministically pick one of the matching input requirements
						resourceTypeMap.put(entry.getKey(), requirement.getItemType());
						break;
					}
				}
			}

			this.resourceTypeMap = resourceTypeMap;
		}

		if (!itemsArray.equals(materialTypeSelect.getItems())) {
			materialTypeSelect.setItems(itemsArray);
			materialTypeSelect.setSelected(itemsArray.get(0));
		}
		resetMaterialSelect();
	}

	private void resetMaterialSelect() {
		currentMaterialNamesMap.clear();
		Array<String> materialTypes = new Array<>();

		String anyString = i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY").toString();
		currentMaterialNamesMap.put(anyString, NULL_MATERIAL);
		materialTypes.add(anyString);

		ItemType itemTypeForMaterialType = resourceTypeMap.get(selectedMaterialType);
		if (itemTypeForMaterialType != null) {
			Set<GameMaterial> materialsByItemType = itemTracker.getMaterialsByItemType(itemTypeForMaterialType);
			if (materialsByItemType != null) {
				for (GameMaterial gameMaterial : materialsByItemType) {
					materialTypes.add(gameMaterial.getMaterialName());
					currentMaterialNamesMap.put(gameMaterial.getMaterialName(), gameMaterial);
				}
			}
		}

		if (materialTypes.contains(materialSelect.getSelected(), false)) {
			// Same selection available so don't change selection
			materialSelect.setItems(materialTypes);
		} else {
			materialSelect.setItems(materialTypes);
			materialSelect.setSelected(materialTypes.get(0));
			selectedMaterial = currentMaterialNamesMap.get(materialTypes.get(0));
		}
		onMaterialSelectionChange();
	}

	public void setCurrentRoomType(RoomType currentRoomType) {
		this.currentRoomType = currentRoomType;
	}

	public RoomType getCurrentRoomType() {
		return currentRoomType;
	}

	@Override
	public void onLanguageUpdated() {
		resetFurnitureTable(); // To catch material type names
		resetMaterialTypeSelect();
	}

	public FurnitureType getSelectedFurnitureType() {
		return selectedFurnitureType;
	}

	public GameMaterial getSelectedMaterial() {
		return selectedMaterial;
	}

	public GameMaterialType getSelectedMaterialType() {
		return selectedMaterialType;
	}
}
