package technology.rocketjump.undermount.assets.viewer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.Map;

public class FurnitureViewerUI implements Disposable {

	private final EntityAssetUpdater entityAssetUpdater;
	private Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json
	private Stage stage;
	private Table containerTable;

	private TextButton backgroundMatButton, mainMatButton;

//	private Map<EntityAssetType, SelectBox> assetSelectWidgets = new HashMap<>();
	private TextButton seedButton;

	private Entity currentEntity;
	private Map<EntityAssetType, EntityAsset> assetMap;
	private FurnitureEntityAttributes entityAttributes;

//	private final CharacterViewPersistentSettings persistentSettings;
	private final EntityAssetTypeDictionary assetTypeDictionary;

	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final PlantSpeciesDictionary plantSpeciesDictionary;

	@Inject
	public FurnitureViewerUI(EntityAssetTypeDictionary assetTypeDictionary, EntityAssetUpdater entityAssetUpdater,
							 FurnitureTypeDictionary furnitureTypeDictionary, GameMaterialDictionary gameMaterialDictionary, PlantSpeciesDictionary plantSpeciesDictionary) {
//		this.persistentSettings = persistentSettings;
		this.assetTypeDictionary = assetTypeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
		stage = new Stage(new ScreenViewport());

		containerTable = new Table(uiSkin);
		containerTable.setFillParent(true);
		stage.addActor(containerTable);

//		containerTable.setDebug(true);
		containerTable.pad(20f); // Table edge padding
		containerTable.left().top();
	}

	public void init(Entity entity) {
		this.currentEntity = entity;
		this.entityAttributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		this.assetMap = entity.getPhysicalEntityComponent().getTypeMap();
//		persistentSettings.reloadFromSettings(entity);
		containerTable.clearChildren();

		createFurnitureTypeWidget();

		containerTable.row();

		createPrimaryMaterialWidget();

		containerTable.row();

		createMaterialWidget(GameMaterialType.STONE);

		containerTable.row();

//		createMaterialWidget(GameMaterialType.METAL);
//
//		containerTable.row();

		createMaterialWidget(GameMaterialType.METAL);

		containerTable.row();

		createMaterialWidget(GameMaterialType.ORE);

		containerTable.row();

		createMaterialWidget(GameMaterialType.GEM);

		containerTable.row();

		createRotateWidget();

		containerTable.row();

//		createTreeSpeciesWidget();
//
//		containerTable.row();

		createSeedWidget();

		containerTable.row();

	}

	private void createSeedWidget() {
		containerTable.add(new Label("Seed: ", uiSkin));
		seedButton = new TextButton("#0", uiSkin);
		seedButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				long nextSeed = entityAttributes.getSeed() + 1L;
				entityAttributes.setSeed(nextSeed);
				entityAssetUpdater.updateEntityAssets(currentEntity);
				seedButton.setText("#" + nextSeed);
			}
		});
		containerTable.add(seedButton);
	}

	private void createRotateWidget() {
		containerTable.add(new Label("Rotate: ", uiSkin));
		seedButton = new TextButton("Rotate me!", uiSkin);
		seedButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				FurnitureLayout currentLayout = entityAttributes.getCurrentLayout();
				if (currentLayout.getRotatesTo() != null) {
					entityAttributes.setCurrentLayout(currentLayout.getRotatesTo());
					entityAssetUpdater.updateEntityAssets(currentEntity);
				}
			}
		});
		containerTable.add(seedButton);
	}

	private void createPrimaryMaterialWidget() {
		SelectBox<String> typeSelect = new SelectBox<>(uiSkin);
		Array<GameMaterialType> types = new Array<>();
		types.add(GameMaterialType.METAL);
		types.add(GameMaterialType.STONE);
		types.add(GameMaterialType.WOOD);

		Array<String> typeNames = new Array<>();
		for (GameMaterialType type : types) {
			typeNames.add(type.name());
		}
		typeSelect.setItems(typeNames);
		typeSelect.setSelected(entityAttributes.getPrimaryMaterialType().name());

		typeSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				String selectedTypeName = typeSelect.getSelected();
				GameMaterialType selectedType = GameMaterialType.valueOf(selectedTypeName);
				entityAttributes.setPrimaryMaterialType(selectedType);
				entityAssetUpdater.updateEntityAssets(currentEntity);
			}
		});
		containerTable.add(new Label("Material Type:", uiSkin), typeSelect);
	}

	private void createFurnitureTypeWidget() {
		SelectBox<String> itemTypeSelect = new SelectBox<>(uiSkin);

		Array<String> furnitureTypeNames = new Array<>();
		for (FurnitureType itemType : furnitureTypeDictionary.getAll()) {
			if (itemType.getName().contains("Null")) {
				continue;
			}
			furnitureTypeNames.add(itemType.getName());
		}
		itemTypeSelect.setItems(furnitureTypeNames);
		itemTypeSelect.setSelected(entityAttributes.getFurnitureType().getName());

		itemTypeSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				String selectedTypeName = itemTypeSelect.getSelected();
				FurnitureType selectedType = furnitureTypeDictionary.getByName(selectedTypeName);

				entityAttributes.setFurnitureType(selectedType);
				entityAssetUpdater.updateEntityAssets(currentEntity);
			}
		});
		containerTable.add(new Label("Item Type:", uiSkin), itemTypeSelect);
	}

	private void createMaterialWidget(GameMaterialType materialType) {
		SelectBox<String> materialSelect = new SelectBox<>(uiSkin);

		Array<String> materialNames = new Array<>();
		for (GameMaterial material : gameMaterialDictionary.getAll()) {
			if (material.getMaterialType().equals(materialType))
				materialNames.add(material.getMaterialName());
		}
		materialSelect.setItems(materialNames);
		materialSelect.setSelected(entityAttributes.getMaterials().get(entityAttributes.getPrimaryMaterialType()).getMaterialName());

		materialSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				String selectedMaterialName = materialSelect.getSelected();
				GameMaterial selectedMaterial = gameMaterialDictionary.getByName(selectedMaterialName);
				entityAttributes.getMaterials().put(selectedMaterial.getMaterialType(), selectedMaterial);
			}
		});
		containerTable.add(new Label(materialType.name() + " material:", uiSkin), materialSelect);
	}


//	private void createTreeSpeciesWidget() {
//		SelectBox<String> treeSelect = new SelectBox<>(uiSkin);
//
//		Array<String> speciesNames = new Array<>();
//		for (PlantSpecies species: plantSpeciesDictionary.getAll()) {
//			if (species.getItem() != null) {
//				speciesNames.add(species.getSpeciesName());
//			}
//		}
//		treeSelect.setItems(speciesNames);
//		treeSelect.setSelected("Oak");
//
//		treeSelect.addListener(new ChangeListener() {
//			public void changed(ChangeEvent event, Actor actor) {
//				String selectedName = treeSelect.getSelected();
//				PlantSpecies plantSpecies = plantSpeciesDictionary.getByName(selectedName);
//
//				entityAttributes.setMaterial(plantSpecies.getBranchMaterial());
//				entityAttributes.setColor(ColoringLayer.BRANCHES_COLOR, plantSpecies.randomBranchColor(new RandomXS128()));
//				entityAttributes.setItemSize(plantSpecies.getItem().getItemSize());
//				entityAttributes.setItemStyle(plantSpecies.getItem().getItemStyle());
//
//				furnitureAssetModifier.resetAssetMap();
//			}
//		});
//		containerTable.add(new Label("Tree species: ", uiSkin), treeSelect);
//	}


	public void render() {
		stage.act();
		stage.draw();
	}

	public void onResize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		stage.dispose();
	}

	public void updateAttributes(FurnitureEntityAttributes entityAttributes) {
		this.entityAttributes = entityAttributes;
	}

	private String colorToString(Color color) {
		return "#" + color.toString().substring(0, 6);
//		return "r:"+String.format("%.4f", color.r)+
//				" g:"+String.format("%.4f", color.g)+
//				" b:"+String.format("%.4f", color.b);
	}

	public Stage getStage() {
		return stage;
	}
}
