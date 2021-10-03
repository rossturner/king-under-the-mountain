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
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyType;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.factories.AccessoryColorFactory;
import technology.rocketjump.undermount.entities.factories.HairColorFactory;
import technology.rocketjump.undermount.entities.factories.SkinColorFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetsByProfession.NULL_ENTITY_ASSET;
import static technology.rocketjump.undermount.assets.entities.model.ColoringLayer.*;

public class CharacterViewUI implements Disposable {

	private final Profession defaultProfession;
	private Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json
	private Stage stage;
	private Table containerTable;

	private TextButton hairColorButton, skinColorButton, accessoryColorButton;

	private Map<EntityAssetType, SelectBox> assetSelectWidgets = new HashMap<>();

	private Entity currentEntity;
	private Map<EntityAssetType, EntityAsset> assetMap;
	private CreatureEntityAttributes entityAttributes;

	private final HairColorFactory hairColorFactory;
	private final SkinColorFactory skinColorFactory;
	private final AccessoryColorFactory accessoryColorFactory;
	private final CreatureEntityAssetDictionary assetDictionary;
	private final CharacterViewPersistentSettings persistentSettings;
	private final EntityAssetTypeDictionary assetTypeDictionary;
	private final EntityAssetUpdater entityAssetUpdater;
	private final ProfessionDictionary professionDictionary;

	@Inject
	public CharacterViewUI(HairColorFactory hairColorFactory, SkinColorFactory skinColorFactory,
						   AccessoryColorFactory accessoryColorFactory,
						   CreatureEntityAssetDictionary assetDictionary, CharacterViewPersistentSettings persistentSettings,
						   EntityAssetTypeDictionary assetTypeDictionary, ProfessionDictionary professionDictionary, EntityAssetUpdater entityAssetUpdater, ProfessionDictionary professionDictionary1) {
		this.hairColorFactory = hairColorFactory;
		this.skinColorFactory = skinColorFactory;
		this.accessoryColorFactory = accessoryColorFactory;
		this.assetDictionary = assetDictionary;
		this.persistentSettings = persistentSettings;
		this.assetTypeDictionary = assetTypeDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
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

	public void init(Entity entity) {
		this.currentEntity = entity;
		this.entityAttributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		this.assetMap = entity.getPhysicalEntityComponent().getTypeMap();
		persistentSettings.reloadFromSettings(entity);
		containerTable.clearChildren();

		createRaceWidgets();

		containerTable.row();

		createGenderWidget();

		containerTable.row();

		createBodyTypeWidget();

		containerTable.row();

		createHairColorWidget();

		containerTable.row();

		createSkinColorWidget();

		containerTable.row();

		createAccessoryColorWidget();

		containerTable.row();

		createProfessionWidget();

		containerTable.row();

		createAssetWidget("Eyebrows", assetTypeDictionary.getByName("HUMANOID_EYEBROWS"));

		containerTable.row();

		createAssetWidget("Beard", assetTypeDictionary.getByName("HUMANOID_BEARD"));

//		containerTable.row();
//
//		createAssetWidget("Head", assetTypeDictionary.getByName("HUMANOID_HEAD"));

		containerTable.row();

		createAssetWidget("Hair", assetTypeDictionary.getByName("HUMANOID_HAIR"));

		containerTable.row();

		createAssetWidget("Clothes", assetTypeDictionary.getByName("BODY_CLOTHING"));

		updateAttributes(entityAttributes);
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
		containerTable.add(professionSelect);
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
				persistentSettings.update(assetType, selectedAsset);
			}
		});
		containerTable.add(widget);
		assetSelectWidgets.put(assetType, widget);
	}

	private void createBodyTypeWidget() {
		containerTable.add(new Label("Body type: ", uiSkin));
		SelectBox<String> bodyTypeSelect = new SelectBox<>(uiSkin);
		Array<String> bodyTypeItems = new Array<>();
		for (CreatureBodyType bodyType : CreatureBodyType.values()) {
			bodyTypeItems.add(bodyType.name().toLowerCase());
		}
		bodyTypeItems.removeValue(CreatureBodyType.ANY.name().toLowerCase(), false);
		bodyTypeSelect.setItems(bodyTypeItems);
		bodyTypeSelect.setSelected(entityAttributes.getBodyType().name().toLowerCase());
		bodyTypeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				CreatureBodyType selectedType = CreatureBodyType.valueOf(bodyTypeSelect.getSelected().toUpperCase());
				entityAttributes.setBodyType(selectedType);
				entityAssetUpdater.updateEntityAssets(currentEntity);
				persistentSettings.setBodyType(selectedType);
				resetAssetSelections();
//				persistentSettings.reloadFromSettings(currentEntity);
			}
		});
		containerTable.add(bodyTypeSelect);
	}

	private void createGenderWidget() {
		SelectBox<String> genderSelect = new SelectBox<>(uiSkin);
		genderSelect.setItems("Male", "Female", "None");
		genderSelect.setSelected(entityAttributes.getGender().equals(Gender.MALE) ? "Male" : "Female");
		genderSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				Gender selectedGender = Gender.valueOf(genderSelect.getSelected().toUpperCase());
				entityAttributes.setGender(selectedGender);
				entityAssetUpdater.updateEntityAssets(currentEntity);
				persistentSettings.setGender(selectedGender);
				resetAssetSelections();
//				persistentSettings.reloadFromSettings(currentEntity);
			}
		});
		containerTable.add(new Label("Gender:", uiSkin), genderSelect);
	}

	private void createSkinColorWidget() {
		containerTable.add(new Label("Skin color: ", uiSkin));
		skinColorButton = new TextButton("#RRGGBB", uiSkin);
		skinColorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Color newColor = skinColorFactory.randomSkinColor(new RandomXS128());
				entityAttributes.setSkinColor(newColor);
				persistentSettings.setSkinColor(newColor);
				updateAttributes(entityAttributes);
			}
		});
		containerTable.add(skinColorButton);
	}

	private void createAccessoryColorWidget() {
		containerTable.add(new Label("Accessory color: ", uiSkin));
		accessoryColorButton = new TextButton("#RRGGBB", uiSkin);
		accessoryColorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Color newColor = accessoryColorFactory.randomAccessoryColor(new RandomXS128());
				entityAttributes.setAccessoryColor(newColor);
				persistentSettings.setAccessoryColor(newColor);
				updateAttributes(entityAttributes);
			}
		});
		containerTable.add(accessoryColorButton);
	}

	private void createHairColorWidget() {
		containerTable.add(new Label("Hair color: ", uiSkin));
		hairColorButton = new TextButton("#rrggbb", uiSkin);
		hairColorButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Color newColor = hairColorFactory.randomHairColor(new RandomXS128());
				entityAttributes.setHairColor(newColor);
				persistentSettings.setHairColor(newColor);
				updateAttributes(entityAttributes);
			}
		});
		containerTable.add(hairColorButton);
	}

	private void createRaceWidgets() {
		SelectBox raceSelect = new SelectBox(uiSkin);
		raceSelect.setItems("Dwarf");
		raceSelect.setSelected("Dwarf");
		containerTable.add(new Label("Race:", uiSkin), raceSelect);
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

	public void updateAttributes(CreatureEntityAttributes entityAttributes) {
		this.entityAttributes = entityAttributes;
		hairColorButton.setColor(entityAttributes.getColor(HAIR_COLOR));
		hairColorButton.setText("#" + entityAttributes.getColor(HAIR_COLOR).toString().substring(0, 6));
		skinColorButton.setColor(entityAttributes.getColor(SKIN_COLOR));
		skinColorButton.setText("#" + entityAttributes.getColor(SKIN_COLOR).toString().substring(0, 6));
		accessoryColorButton.setColor(entityAttributes.getColor(ACCESSORY_COLOR));
		accessoryColorButton.setText("#" + entityAttributes.getColor(ACCESSORY_COLOR).toString().substring(0, 6));
	}

	public Stage getStage() {
		return stage;
	}
}
