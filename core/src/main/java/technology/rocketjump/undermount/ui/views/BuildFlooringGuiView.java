package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.MaterialSelectionMessage;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.actions.SetInteractionMode;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.*;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

@Singleton
public class BuildFlooringGuiView implements GuiView, I18nUpdatable {

	private final ItemTracker itemTracker;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;


	private Table viewTable;
	private final SelectBox<GameMaterialType> materialTypeSelect;
	private final SelectBox<String> materialSelect;
	// I18nUpdatable
	private final Label headingLabel;
	private final Label typeLabel;
	private final Label materialLabel;

	private final Map<GameMaterialType, ItemType> resourceTypeMap = new TreeMap<>();
	private final Map<String, GameMaterial> currentMaterialNamesMap = new HashMap<>();
	private final List<IconButton> iconButtons = new LinkedList<>();

	private GameMaterialType selectedMaterialType;
	private GameMaterial selectedMaterial;
	boolean initialised;

	@Inject
	public BuildFlooringGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
								IconButtonFactory iconButtonFactory, FloorTypeDictionary floorTypeDictionary, ItemTracker itemTracker,
								I18nTranslator i18nTranslator, I18nWidgetFactory i18NWidgetFactory) {
		this.messageDispatcher = messageDispatcher;
		this.itemTracker = itemTracker;
		this.i18nTranslator = i18nTranslator;

		for (FloorType floorType : floorTypeDictionary.getAllDefinitions()) {
			if (floorType.isConstructed()) {
				List<QuantifiedItemType> requirements = floorType.getRequirements().get(floorType.getMaterialType());
				if (requirements.size() == 1 && requirements.get(0).getQuantity() == 1) {
					resourceTypeMap.put(floorType.getMaterialType(), requirements.get(0).getItemType());
				} else {
					Logger.error(floorType.getFloorTypeName() + " must only have a single requirements ingredient");
				}
			}
		}

		Skin uiSkin = guiSkinRepository.getDefault();

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");
//		viewTable.setDebug(true);


		headingLabel = i18NWidgetFactory.createLabel("GUI.BUILD.FLOOR");
		viewTable.add(headingLabel).center().colspan(2);
		viewTable.row();

		Table materialTable = new Table(uiSkin);

		this.typeLabel = i18NWidgetFactory.createLabel("MATERIAL_TYPE");
		materialTable.add(typeLabel);
		materialTable.row();


		materialTypeSelect = new SelectBox<>(uiSkin);
		materialTypeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				selectedMaterialType = materialTypeSelect.getSelected();
				resetMaterialSelect();
			}
		});
		materialTable.add(materialTypeSelect).pad(5);
		materialTable.row();

		this.materialLabel = i18NWidgetFactory.createLabel("MATERIAL");
		materialTable.add(materialLabel);
		materialTable.row();


		materialSelect = new SelectBox<>(uiSkin);
		Array<String> materialNamesArray = new Array<>();
		materialNamesArray.add("SOMETHING");
		materialSelect.setItems(materialNamesArray);
		materialSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				onMaterialSelectionChange();
			}
		});

		materialTable.add(materialSelect).pad(5);
		materialTable.row();


		viewTable.add(materialTable);

		Table iconTable = new Table(uiSkin);

		IconButton floorIconButton = iconButtonFactory.create("GUI.BUILD.FLOOR", "floorboards", HexColors.get("#F1F1E0"), ButtonStyle.DEFAULT);
		floorIconButton.setAction(new SetInteractionMode(GameInteractionMode.PLACE_FLOORING, messageDispatcher));
		iconButtons.add(floorIconButton);

		for (IconButton iconButton : iconButtons) {
			iconTable.add(iconButton).pad(5);
		}

		viewTable.add(iconTable);

		viewTable.row();

		resetMaterialTypeSelect();
		onLanguageUpdated();
		initialised = true;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BUILD_FLOORING;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.BUILD_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();

		resetMaterialSelect();

		containerTable.add(viewTable);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

	@Override
	public void onLanguageUpdated() {
		resetMaterialTypeSelect(); // For different material names
	}

	private void resetMaterialTypeSelect() {
		Array<GameMaterialType> itemsArray = new Array<>();
		for (GameMaterialType gameMaterialType : resourceTypeMap.keySet()) {
			itemsArray.add(gameMaterialType);
		}
		materialTypeSelect.setItems(itemsArray);
		materialTypeSelect.setSelected(itemsArray.get(0));
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

		materialSelect.setItems(materialTypes);
		if (materialTypes.size > 0) {
			materialSelect.setSelected(materialTypes.get(0));
			selectedMaterial = currentMaterialNamesMap.get(materialTypes.get(0));
		}
		onMaterialSelectionChange();
	}

	private void onMaterialSelectionChange() {
		selectedMaterial = currentMaterialNamesMap.get(materialSelect.getSelected());
		if (initialised) {
			messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
					selectedMaterialType, selectedMaterial, resourceTypeMap.get(selectedMaterialType)));
		}
	}
}
