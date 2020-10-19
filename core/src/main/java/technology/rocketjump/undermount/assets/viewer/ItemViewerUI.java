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
import technology.rocketjump.undermount.assets.entities.item.model.ItemSize;
import technology.rocketjump.undermount.assets.entities.item.model.ItemStyle;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.Map;

public class ItemViewerUI implements Disposable {

	private final EntityAssetUpdater entityAssetUpdater;
	private Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json
	private Stage stage;
	private Table containerTable;

	private TextButton backgroundMatButton, mainMatButton;

//	private Map<EntityAssetType, SelectBox> assetSelectWidgets = new HashMap<>();
	private TextButton seedButton;

	private Entity currentEntity;
	private Map<EntityAssetType, EntityAsset> assetMap;
	private ItemEntityAttributes entityAttributes;

//	private final CharacterViewPersistentSettings persistentSettings;
	private final EntityAssetTypeDictionary assetTypeDictionary;

	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final PlantSpeciesDictionary plantSpeciesDictionary;

	@Inject
	public ItemViewerUI(EntityAssetUpdater entityAssetUpdater, EntityAssetTypeDictionary assetTypeDictionary,
						ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary gameMaterialDictionary, PlantSpeciesDictionary plantSpeciesDictionary) {
		this.entityAssetUpdater = entityAssetUpdater;
//		this.persistentSettings = persistentSettings;
		this.assetTypeDictionary = assetTypeDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
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
		this.entityAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		this.assetMap = entity.getPhysicalEntityComponent().getTypeMap();
//		persistentSettings.reloadFromSettings(entity);
		containerTable.clearChildren();

		createItemTypeWidget();

		containerTable.row();

		createItemSizeWidget();

		containerTable.row();

		createItemStyleWidget();

		containerTable.row();

		createMaterialWidget(GameMaterialType.STONE);

		containerTable.row();

		createMaterialWidget(GameMaterialType.ORE);

		containerTable.row();

		createMaterialWidget(GameMaterialType.GEM);

		containerTable.row();

		createMaterialWidget(GameMaterialType.WOOD);

		containerTable.row();

		createMaterialWidget(GameMaterialType.METAL);

		containerTable.row();

		createTreeSpeciesWidget();

		containerTable.row();

		createQuantityWidget();

		containerTable.row();

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

	private void createItemSizeWidget() {
		containerTable.add(new Label("Size: ", uiSkin));
		SelectBox<String> itemSizeSelect = new SelectBox<>(uiSkin);

		Array<String> sizeStrings = new Array<>();
		for (ItemSize itemSize : ItemSize.values()) {
			sizeStrings.add(itemSize.name());
		}
		itemSizeSelect.setItems(sizeStrings);
		itemSizeSelect.setSelected(String.valueOf(entityAttributes.getItemSize()));
		itemSizeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				ItemSize selectedSize = ItemSize.valueOf(itemSizeSelect.getSelected());
				entityAttributes.setItemSize(selectedSize);
				entityAssetUpdater.updateEntityAssets(currentEntity);
			}
		});
		containerTable.add(itemSizeSelect);
	}

	private void createItemStyleWidget() {
		containerTable.add(new Label("Style: ", uiSkin));
		SelectBox<String> itemStyleSelect = new SelectBox<>(uiSkin);

		Array<String> styleStrings = new Array<>();
		for (ItemStyle itemStyle : ItemStyle.values()) {
			styleStrings.add(itemStyle.name());
		}
		itemStyleSelect.setItems(styleStrings);
		itemStyleSelect.setSelected(String.valueOf(entityAttributes.getItemStyle()));
		itemStyleSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				ItemStyle selectedStyle = ItemStyle.valueOf(itemStyleSelect.getSelected());
				entityAttributes.setItemStyle(selectedStyle);
				entityAssetUpdater.updateEntityAssets(currentEntity);
			}
		});
		containerTable.add(itemStyleSelect);
	}

	private void createItemTypeWidget() {
		SelectBox<String> itemTypeSelect = new SelectBox<>(uiSkin);

		Array<String> itemTypeNames = new Array<>();
		for (ItemType itemType : itemTypeDictionary.getAll()) {
			itemTypeNames.add(itemType.getItemTypeName());
		}
		itemTypeSelect.setItems(itemTypeNames);
		itemTypeSelect.setSelected(entityAttributes.getItemType().getItemTypeName());

		itemTypeSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				String selectedTypeName = itemTypeSelect.getSelected();
				ItemType selectedType = itemTypeDictionary.getByName(selectedTypeName);

				entityAttributes.setItemType(selectedType);

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
		materialSelect.setSelected(entityAttributes.getMaterial(materialType).getMaterialName());

		materialSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				String selectedMaterialName = materialSelect.getSelected();
				GameMaterial selectedMaterial = gameMaterialDictionary.getByName(selectedMaterialName);
				entityAttributes.setMaterial(selectedMaterial);
			}
		});
		containerTable.add(new Label(materialType.name() + " material:", uiSkin), materialSelect);
	}


	private void createTreeSpeciesWidget() {
		SelectBox<String> treeSelect = new SelectBox<>(uiSkin);

		Array<String> speciesNames = new Array<>();
		for (PlantSpecies species: plantSpeciesDictionary.getAll()) {
			if (species.anyStageProducesItem()) {
				speciesNames.add(species.getSpeciesName());
			}
		}
		treeSelect.setItems(speciesNames);
		treeSelect.setSelected("Oak");

		treeSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				String selectedName = treeSelect.getSelected();
				PlantSpecies plantSpecies = plantSpeciesDictionary.getByName(selectedName);

				entityAttributes.setMaterial(plantSpecies.getMaterial());
				entityAttributes.setColor(ColoringLayer.BRANCHES_COLOR, plantSpecies.getMaterial().getColor());
				for (PlantSpeciesGrowthStage plantSpeciesGrowthStage : plantSpecies.getGrowthStages()) {
					if (!plantSpeciesGrowthStage.getHarvestedItems().isEmpty()) {
						entityAttributes.setItemSize(plantSpeciesGrowthStage.getHarvestedItems().get(0).getItemSize());
						entityAttributes.setItemStyle(plantSpeciesGrowthStage.getHarvestedItems().get(0).getItemStyle());
						break;
					}
				}

				entityAssetUpdater.updateEntityAssets(currentEntity);
			}
		});
		containerTable.add(new Label("Tree species: ", uiSkin), treeSelect);
	}

	private void createQuantityWidget() {
		SelectBox<String> quantitySelect = new SelectBox<>(uiSkin);

		Array<String> quantityNames = new Array<>();
		for (int i = 1; i <= 25; i++) {
			quantityNames.add(String.valueOf(i));
		}
		quantitySelect.setItems(quantityNames);
		quantitySelect.setSelected(String.valueOf(entityAttributes.getQuantity()));

		quantitySelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				int selectedQuantity = Integer.valueOf(quantitySelect.getSelected());
				entityAttributes.setQuantity(selectedQuantity);
				entityAssetUpdater.updateEntityAssets(currentEntity);
			}
		});
		containerTable.add(new Label("Quantity: ", uiSkin), quantitySelect);
	}

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

	public void updateAttributes(ItemEntityAttributes entityAttributes) {
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
