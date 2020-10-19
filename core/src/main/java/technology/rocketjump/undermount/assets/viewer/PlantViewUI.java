package technology.rocketjump.undermount.assets.viewer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;

public class PlantViewUI implements Disposable {

	private Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json
	private Stage stage;
	private Table containerTable;

	private TextButton branchColorButton, leafColorButton;

	private Entity currentEntity;
	private PlantEntityAttributes entityAttributes;

	private final EntityAssetUpdater entityAssetUpdater;

	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private SelectBox<String> growthStageSelect;

	@Inject
	public PlantViewUI(EntityAssetUpdater entityAssetUpdater, PlantSpeciesDictionary plantSpeciesDictionary) {
		this.entityAssetUpdater = entityAssetUpdater;
//		this.persistentSettings = persistentSettings;
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
		this.entityAttributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		containerTable.clearChildren();

		createSpeciesWidget();

		containerTable.row();

		createGrowthStageWidget();

		containerTable.row();

		createBranchColorOffsetWidget();

		containerTable.row();

		createLeafOffsetColorWidget();

		containerTable.row();

		attributesUpdated(entityAttributes);
	}

	private void createGrowthStageWidget() {
		containerTable.add(new Label("Growth stage: ", uiSkin));
		growthStageSelect = new SelectBox<>(uiSkin);
		Array<String> choices = new Array<>();
		for (int cursor = 0; cursor < entityAttributes.getSpecies().getGrowthStages().size(); cursor++) {
			choices.add(String.valueOf(cursor));
		}
		growthStageSelect.setItems(choices);
		growthStageSelect.setSelected(String.valueOf(entityAttributes.getGrowthStageCursor()));
		growthStageSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int selectedStage = Integer.parseInt(growthStageSelect.getSelected());
				entityAttributes.setGrowthStageCursor(selectedStage);
				attributesUpdated(entityAttributes);
			}
		});
		containerTable.add(growthStageSelect);
	}

	private void createSpeciesWidget() {
		SelectBox<String> speciesSelect = new SelectBox<>(uiSkin);

		Array<String> speciesNames = new Array<>();
		for (PlantSpecies plantSpecies : plantSpeciesDictionary.getAll()) {
			speciesNames.add(plantSpecies.getSpeciesName());
		}
		speciesSelect.setItems(speciesNames);
		speciesSelect.setSelected(entityAttributes.getSpecies().getSpeciesName());

		speciesSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				String selectedSpeciesName = speciesSelect.getSelected();
				PlantSpecies selectedSpecies = plantSpeciesDictionary.getByName(selectedSpeciesName);

				entityAttributes = new PlantEntityAttributes(entityAttributes.getSeed(), selectedSpecies);
				attributesUpdated(entityAttributes);


				Array<String> choices = new Array<>();
				for (int cursor = 0; cursor < entityAttributes.getSpecies().getGrowthStages().size(); cursor++) {
					choices.add(String.valueOf(cursor));
				}
				growthStageSelect.setItems(choices);
				growthStageSelect.setSelected(String.valueOf(entityAttributes.getGrowthStageCursor()));
			}
		});
		containerTable.add(new Label("Species:", uiSkin), speciesSelect);
	}

	private void createLeafOffsetColorWidget() {
		containerTable.add(new Label("Leaf color: ", uiSkin));
		leafColorButton = new TextButton("#rrggbb", uiSkin);
		leafColorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				entityAttributes = new PlantEntityAttributes(new RandomXS128().nextLong(), entityAttributes.getSpecies());
				attributesUpdated(entityAttributes);
			}
		});
		containerTable.add(leafColorButton);
	}

	private void createBranchColorOffsetWidget() {
		containerTable.add(new Label("Branch color: ", uiSkin));
		branchColorButton = new TextButton("#rrggbb", uiSkin);
		branchColorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				entityAttributes = new PlantEntityAttributes(new RandomXS128().nextLong(), entityAttributes.getSpecies());
				attributesUpdated(entityAttributes);
			}
		});
		containerTable.add(branchColorButton);
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

	public void attributesUpdated(PlantEntityAttributes entityAttributes) {
		currentEntity.getPhysicalEntityComponent().setAttributes(entityAttributes);

		entityAttributes.updateColors(null);

		Color branchColor = entityAttributes.getColor(ColoringLayer.BRANCHES_COLOR);
		branchColorButton.setColor(branchColor.cpy());
		branchColorButton.setText(colorToString(branchColor));
		Color leafColor = entityAttributes.getColor(ColoringLayer.LEAF_COLOR);
		if (leafColor != null) {
			leafColorButton.setColor(leafColor.cpy());
			leafColorButton.setText(colorToString(leafColor));
		}

		entityAssetUpdater.updateEntityAssets(currentEntity);
	}

	private String colorToString(Color color) {
		return "#" + color.toString().substring(0, 6);
	}

	public Stage getStage() {
		return stage;
	}
}
