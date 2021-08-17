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
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.undermount.mapping.tile.underground.PipeLayout;
import technology.rocketjump.undermount.mapping.tile.underground.PipeLayoutAtlas;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.Map;

public class MechanismViewerUI implements Disposable {

	private final EntityAssetUpdater entityAssetUpdater;
	private Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json
	private Stage stage;
	private Table containerTable;

	private TextButton backgroundMatButton, mainMatButton;

//	private Map<EntityAssetType, SelectBox> assetSelectWidgets = new HashMap<>();
	private TextButton seedButton;

	private Entity currentEntity;
	private Map<EntityAssetType, EntityAsset> assetMap;
	private MechanismEntityAttributes entityAttributes;

//	private final CharacterViewPersistentSettings persistentSettings;
	private final EntityAssetTypeDictionary assetTypeDictionary;

	private final MechanismTypeDictionary mechanismTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final PipeLayoutAtlas pipeLayoutAtlas;

	@Inject
	public MechanismViewerUI(EntityAssetTypeDictionary assetTypeDictionary, EntityAssetUpdater entityAssetUpdater,
							 MechanismTypeDictionary mechanismTypeDictionary, GameMaterialDictionary gameMaterialDictionary, PipeLayoutAtlas pipeLayoutAtlas) {
//		this.persistentSettings = persistentSettings;
		this.assetTypeDictionary = assetTypeDictionary;
		this.mechanismTypeDictionary = mechanismTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
		this.pipeLayoutAtlas = pipeLayoutAtlas;
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

		this.entityAttributes = (MechanismEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		this.assetMap = entity.getPhysicalEntityComponent().getTypeMap();
//		persistentSettings.reloadFromSettings(entity);
		containerTable.clearChildren();

		createMechanismTypeWidget();

		containerTable.row();

		createMaterialWidget(GameMaterialType.STONE);

		containerTable.row();

//		createMaterialWidget(GameMaterialType.METAL);
//
//		containerTable.row();

		createMaterialWidget(GameMaterialType.WOOD);

		containerTable.row();

		createMaterialWidget(GameMaterialType.ORE);

		containerTable.row();

		createMaterialWidget(GameMaterialType.GEM);

		containerTable.row();

		createLayoutWidget();

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

	private void createLayoutWidget() {
		SelectBox<Integer> pipeLayoutSelect = new SelectBox<>(uiSkin);

		Array<Integer> layouts = new Array<>();
		for (Integer layout : pipeLayoutAtlas.getUniqueLayouts()) {
			layouts.add(layout);
		}
		pipeLayoutSelect.setItems(layouts);
		pipeLayoutSelect.setSelected(entityAttributes.getPipeLayout().getId());

		pipeLayoutSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				Integer selectedLayout = pipeLayoutSelect.getSelected();
				entityAttributes.setPipeLayout(new PipeLayout(selectedLayout));
				entityAssetUpdater.updateEntityAssets(currentEntity);
			}
		});
		containerTable.add(new Label("Pipe Layout: ", uiSkin), pipeLayoutSelect);
	}

	private void createMechanismTypeWidget() {
		SelectBox<String> itemTypeSelect = new SelectBox<>(uiSkin);

		Array<String> mechanismTypeNames = new Array<>();
		for (MechanismType mechanismType : mechanismTypeDictionary.getAll()) {
			if (mechanismType.getName().contains("Null")) {
				continue;
			}
			mechanismTypeNames.add(mechanismType.getName());
		}
		itemTypeSelect.setItems(mechanismTypeNames);
		itemTypeSelect.setSelected(entityAttributes.getMechanismType().getName());

		itemTypeSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				String selectedTypeName = itemTypeSelect.getSelected();
				MechanismType selectedType = mechanismTypeDictionary.getByName(selectedTypeName);

				entityAttributes.setMechanismType(selectedType);
				entityAssetUpdater.updateEntityAssets(currentEntity);
			}
		});
		containerTable.add(new Label("Mechanism Type:", uiSkin), itemTypeSelect);
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

	public void updateAttributes(MechanismEntityAttributes entityAttributes) {
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
