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
import org.apache.commons.lang3.text.WordUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.*;
import technology.rocketjump.undermount.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetsByProfession.NULL_ENTITY_ASSET;

public class CreatureViewUI implements Disposable {

	private final Profession defaultProfession;
	private Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json
	private Stage stage;
	private Table containerTable;

	private Map<EntityAssetType, SelectBox> assetSelectWidgets = new HashMap<>();

	private Entity currentEntity;
	private Map<EntityAssetType, EntityAsset> assetMap;
	private CreatureEntityAttributes entityAttributes;

	private final CreatureEntityAssetDictionary assetDictionary;
	private final EntityAssetTypeDictionary assetTypeDictionary;
	private final EntityAssetUpdater entityAssetUpdater;
	private final RaceDictionary raceDictionary;
	private final ProfessionDictionary professionDictionary;
	private SelectBox<String> bodyTypeSelect;

	@Inject
	public CreatureViewUI(CreatureEntityAssetDictionary assetDictionary,
						  EntityAssetTypeDictionary assetTypeDictionary, ProfessionDictionary professionDictionary,
						  EntityAssetUpdater entityAssetUpdater, RaceDictionary raceDictionary,
						  ProfessionDictionary professionDictionary1) {
		this.assetDictionary = assetDictionary;
		this.assetTypeDictionary = assetTypeDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
		this.raceDictionary = raceDictionary;
		this.professionDictionary = professionDictionary1;
		stage = new Stage(new ScreenViewport());

		containerTable = new Table(uiSkin);
		containerTable.setFillParent(true);
		stage.addActor(containerTable);

		defaultProfession = professionDictionary.getByName("VILLAGER");

//		containerTable.setDebug(true);
		containerTable.pad(20f); // Table edge padding
		containerTable.left().top();
	}

	public void reset(Entity entity) {
		this.currentEntity = entity;
		this.entityAttributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		this.assetMap = entity.getPhysicalEntityComponent().getTypeMap();
		containerTable.clearChildren();

		createRaceWidget();

		createGenderWidget();

		createBodyShapeWidget();

//		createHairColorWidget();
		// TODO replace with random widget for each color layer

//		containerTable.row();

		createProfessionWidget();

		for (String assetTypeName : List.of("CREATURE_EYEBROWS", "CREATURE_BEARD", "CREATURE_HAIR", "BODY_CLOTHING")) {
			EntityAssetType assetType = assetTypeDictionary.getByName(assetTypeName);
			if (assetMap.containsKey(assetType)) {
				String label = WordUtils.capitalize(assetTypeName.substring(assetTypeName.indexOf("_") + 1).toLowerCase());
				createAssetWidget(label, assetType);
			}
		}

		for (Map.Entry<ColoringLayer, Color> colorEntry : entityAttributes.getColors().entrySet()) {
			createColorWidget(colorEntry.getKey());
		}

	}

	private void createRaceWidget() {
		SelectBox<String> raceSelect = new SelectBox<>(uiSkin);
		Array<String> items = new Array<>();
		for (Race race : raceDictionary.getAll()) {
			items.add(race.getName());
		}
		raceSelect.setItems(items);
		raceSelect.setSelected(entityAttributes.getRace().getName());
		raceSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Race race = raceDictionary.getByName(raceSelect.getSelected());
				raceChanged(race);
			}
		});

		containerTable.add(new Label("Race:", uiSkin), raceSelect).row();
	}

	private void raceChanged(Race race) {
		entityAttributes = new CreatureEntityAttributes(race, 1L);

		// Might need to do something with ProfessionsComponent dependent on race behaviour

		currentEntity.getPhysicalEntityComponent().setAttributes(entityAttributes);
		entityAssetUpdater.updateEntityAssets(currentEntity);
		reset(currentEntity);
	}

	private void createProfessionWidget() {
		containerTable.add(new Label("Profession: ", uiSkin));
		SelectBox<Profession> professionSelect = new SelectBox<>(uiSkin);
		Array<Profession> professionArray = new Array<>();
		for (Profession profession : professionDictionary.getAll()) {
			professionArray.add(profession);
		}

		professionSelect.setItems(professionArray);
		ProfessionsComponent component = currentEntity.getComponent(ProfessionsComponent.class);
		professionSelect.setSelected(component.getPrimaryProfession(professionDictionary.getDefault()));
		professionSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Profession selected = professionSelect.getSelected();
				ProfessionsComponent professionsComponent = currentEntity.getComponent(ProfessionsComponent.class);
				professionsComponent.clear();
				professionsComponent.add(selected, 0.5f);
				entityAssetUpdater.updateEntityAssets(currentEntity);
				resetAssetSelections();
//				persistentSettings.reloadFromSettings(currentEntity);
			}
		});
		containerTable.add(professionSelect).row();
	}

	private void resetAssetSelections() {
		Profession primaryProfession = currentEntity.getComponent(ProfessionsComponent.class).getPrimaryProfession(defaultProfession);
		for (Map.Entry<EntityAssetType, SelectBox> entry : assetSelectWidgets.entrySet()) {
			Array<String> newItems = new Array<>();
			for (CreatureEntityAsset entityAsset : assetDictionary.getAllMatchingAssets(entry.getKey(), entityAttributes, primaryProfession)) {
				newItems.add(entityAsset.getUniqueName());
			}
			newItems.add("None");
			entry.getValue().setItems(newItems);
			CreatureEntityAsset entityAsset = (CreatureEntityAsset) assetMap.get(entry.getKey());
			if (entityAsset != null) {
				entry.getValue().setSelected(entityAsset.getUniqueName());
			}
		}
	}

	private void createAssetWidget(String label, EntityAssetType assetType) {
		containerTable.add(new Label(label + ": ", uiSkin));
		SelectBox<String> widget = new SelectBox<>(uiSkin);
		Array<String> assetNames = new Array<>();

		Profession primaryProfession = currentEntity.getComponent(ProfessionsComponent.class).getPrimaryProfession(defaultProfession);
		List<CreatureEntityAsset> matchingAssetsWithSameType = assetDictionary.getAllMatchingAssets(assetType, entityAttributes, primaryProfession);
		for (CreatureEntityAsset asset : matchingAssetsWithSameType) {
			assetNames.add(asset.getUniqueName());
		}
		assetNames.add("None");
		widget.setItems(assetNames);
		if (assetMap.get(assetType) != null) {
			CreatureEntityAsset entityAsset = (CreatureEntityAsset) assetMap.get(assetType);
			widget.setSelected(entityAsset.getUniqueName());
		}
		widget.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				List<CreatureEntityAsset> matchingAssetsWithSameType = assetDictionary.getAllMatchingAssets(assetType, entityAttributes, primaryProfession);
				Optional<CreatureEntityAsset> matchedAsset = matchingAssetsWithSameType.stream()
						.filter(asset -> asset.getUniqueName().equalsIgnoreCase(widget.getSelected()))
						.findFirst();

				CreatureEntityAsset selectedAsset;
				if (matchedAsset.isPresent()) {
					selectedAsset = matchedAsset.get();
				} else if (widget.getSelected().equalsIgnoreCase("None")) {
					selectedAsset = NULL_ENTITY_ASSET;
				} else {
					Logger.error("Error: Could not find asset with name " + widget.getSelected());
					return;
				}
				assetMap.put(assetType, selectedAsset);
			}
		});
		containerTable.add(widget).row();
		assetSelectWidgets.put(assetType, widget);
	}

	private void createBodyShapeWidget() {
		containerTable.add(new Label("Body shape: ", uiSkin));
		bodyTypeSelect = new SelectBox<>(uiSkin);
		resetBodyShapeSelect();
		bodyTypeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				CreatureBodyShape selectedType = CreatureBodyShape.valueOf(bodyTypeSelect.getSelected());
				entityAttributes.setBodyShape(selectedType);
				entityAssetUpdater.updateEntityAssets(currentEntity);
				resetAssetSelections();
//				persistentSettings.reloadFromSettings(currentEntity);
			}
		});
		containerTable.add(bodyTypeSelect).row();
	}

	private void resetBodyShapeSelect() {
		Array<String> bodyTypeItems = new Array<>();
		for (CreatureBodyShapeDescriptor bodyShape : entityAttributes.getRace().getBodyShapes()) {
			bodyTypeItems.add(bodyShape.getValue().name());
		}
		bodyTypeSelect.setItems(bodyTypeItems);
		bodyTypeSelect.setSelected(entityAttributes.getBodyShape().name().toLowerCase());
	}

	private void createGenderWidget() {
		SelectBox<String> genderSelect = new SelectBox<>(uiSkin);
		Array<String> items = new Array<>();
		for (Map.Entry<Gender, RaceGenderDescriptor> genderEntry : entityAttributes.getRace().getGenders().entrySet()) {
			items.add(genderEntry.getKey().name());
		}
		genderSelect.setItems(items);
		genderSelect.setSelected(entityAttributes.getGender().name());
		genderSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				Gender selectedGender = Gender.valueOf(genderSelect.getSelected());
				entityAttributes.setGender(selectedGender);
				entityAssetUpdater.updateEntityAssets(currentEntity);
				resetAssetSelections();
			}
		});
		containerTable.add(new Label("Gender:", uiSkin), genderSelect).row();
	}

	private void createColorWidget(ColoringLayer coloringLayer) {
		containerTable.add(new Label(WordUtils.capitalize(coloringLayer.name().toLowerCase()) + " color: ", uiSkin));
		TextButton colorButton = new TextButton(HexColors.toHexString(entityAttributes.getColor(coloringLayer)), uiSkin);
		colorButton.setColor(entityAttributes.getColor(coloringLayer));
		colorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				SpeciesColor speciesColor = entityAttributes.getRace().getColors().get(coloringLayer);
				entityAttributes.getColors().put(coloringLayer, speciesColor.getColor(new RandomXS128().nextLong()));
				colorButton.setText(HexColors.toHexString(entityAttributes.getColor(coloringLayer)));
				colorButton.setColor(entityAttributes.getColor(coloringLayer));
			}
		});
		containerTable.add(colorButton).row();
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

	public Stage getStage() {
		return stage;
	}
}
