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
import technology.rocketjump.undermount.assets.WallTypeDictionary;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.mapping.model.WallPlacementMode;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.MaterialSelectionMessage;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.List;
import java.util.*;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

@Singleton
public class BuildWallsGuiView implements GuiView, I18nUpdatable {

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
	private final TextButton backButton;

	private final Map<GameMaterialType, ItemType> resourceTypeMap = new TreeMap<>();
	private final Map<String, GameMaterial> currentMaterialNamesMap = new HashMap<>();
	private final List<IconButton> iconButtons = new LinkedList<>();

	private GameMaterialType selectedMaterialType;
	private GameMaterial selectedMaterial;
	boolean initialised;

	@Inject
	public BuildWallsGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							 IconButtonFactory iconButtonFactory, WallTypeDictionary wallTypeDictionary, ItemTracker itemTracker,
							 I18nTranslator i18nTranslator, I18nWidgetFactory i18NWidgetFactory) {
		this.messageDispatcher = messageDispatcher;
		this.itemTracker = itemTracker;
		this.i18nTranslator = i18nTranslator;

		for (WallType wallType : wallTypeDictionary.getAllDefinitions()) {
			if (wallType.isConstructed()) {
				List<QuantifiedItemType> requirements = wallType.getRequirements().get(wallType.getMaterialType());
				if (requirements.size() == 1) {
					resourceTypeMap.put(wallType.getMaterialType(), requirements.get(0).getItemType());
				} else {
					Logger.error(wallType.getWallTypeName() + " must only have a single requirements ingredient");
				}
			}
		}

		Skin uiSkin = guiSkinRepository.getDefault();

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");
//		viewTable.setDebug(true);


		headingLabel = i18NWidgetFactory.createLabel("GUI.BUILD.WALLS");
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

		IconButton single = iconButtonFactory.create("GUI.BUILD.WALLS.SINGLE", "straight-line", HexColors.get("#DDF1E0"), ButtonStyle.DEFAULT);
		single.setAction(() -> messageDispatcher.dispatchMessage(MessageType.WALL_PLACEMENT_SELECTED, WallPlacementMode.STRAIGHT_LINE));
		iconButtons.add(single);

		IconButton lShape = iconButtonFactory.create("GUI.BUILD.WALLS.LSHAPE", "l-shape", HexColors.get("#DDF0F1"), ButtonStyle.DEFAULT);
		lShape.setAction(() -> messageDispatcher.dispatchMessage(MessageType.WALL_PLACEMENT_SELECTED, WallPlacementMode.L_SHAPE));
		iconButtons.add(lShape);

		IconButton quad = iconButtonFactory.create("GUI.BUILD.WALLS.QUAD", "square", HexColors.get("#c8d1ec"), ButtonStyle.DEFAULT);
		quad.setAction(() -> messageDispatcher.dispatchMessage(MessageType.WALL_PLACEMENT_SELECTED, WallPlacementMode.QUAD));
		iconButtons.add(quad);

		for (IconButton iconButton : iconButtons) {
			iconTable.add(iconButton).pad(5);
		}


		viewTable.add(iconTable);

		viewTable.row();

		backButton = i18NWidgetFactory.createTextButton("GUI.BACK_LABEL");
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.BUILD_MENU);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
			}
		});
		viewTable.add(backButton).pad(10).left().colspan(1);

		resetMaterialTypeSelect();
		onLanguageUpdated();
		initialised = true;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BUILD_WALLS;
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
			messageDispatcher.dispatchMessage(MessageType.WALL_MATERIAL_SELECTED, new MaterialSelectionMessage(
					selectedMaterialType, selectedMaterial, resourceTypeMap.get(selectedMaterialType)));
		}
	}
}
