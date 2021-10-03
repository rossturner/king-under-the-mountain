package technology.rocketjump.undermount.assets.viewer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyType;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.assets.entities.creature.CreatureEntityAssetsByProfession.NULL_ENTITY_ASSET;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterViewPersistentSettings {

	private Gender gender;
	private Race race = Race.DWARF;
	private CreatureBodyType bodyType;
	private Map<EntityAssetType, String> typeToUniqueNameMap = new HashMap<>();
	private Color skinColor, hairColor, accessoryColor;

	@JsonIgnore
	private final CreatureEntityAssetDictionary assetDictionary;
	@JsonIgnore
	private ObjectMapper mapper = new ObjectMapper();
	@JsonIgnore
	private final FileHandle settingsFile;
	@JsonIgnore
	private boolean isPersistable = false;
	private EntityAssetType humanoidBody = new EntityAssetType("HUMANOID_BODY");

	public CharacterViewPersistentSettings() {
		// For Jackson only
		assetDictionary = null;
		settingsFile = null;
	}

	@Inject
	public CharacterViewPersistentSettings(CreatureEntityAssetDictionary assetDictionary) throws IOException {
		isPersistable = true;
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		this.assetDictionary = assetDictionary;
		settingsFile = Gdx.files.internal("assets/character-viewer-settings.json");

		if (settingsFile.exists()) {
			String fileAsString = FileUtils.readFileToString(settingsFile.file(), "UTF-8");
			CharacterViewPersistentSettings persisted = mapper.readValue(fileAsString, this.getClass());
			this.gender = persisted.gender;
			this.race = persisted.race;
			this.bodyType = persisted.bodyType;
			this.typeToUniqueNameMap = persisted.typeToUniqueNameMap;
			this.skinColor = persisted.skinColor;
			this.hairColor = persisted.hairColor;
			this.accessoryColor = persisted.accessoryColor;
		} else {
			persist();
		}
	}

	public void reloadFromSettings(Entity entity) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		Map<EntityAssetType, EntityAsset> assetMap = entity.getPhysicalEntityComponent().getTypeMap();

		if (race != null) {
			attributes.setRace(race);
		}

		if (gender != null) {
			attributes.setGender(gender);
		}

		if (bodyType != null) {
			attributes.setBodyType(bodyType);
		}

		if (hairColor != null) {
			attributes.setHairColor(hairColor);
		}
		if (skinColor != null) {
			attributes.setSkinColor(skinColor);
		}
		if (accessoryColor != null) {
			attributes.setAccessoryColor(accessoryColor);
		}

		ProfessionsComponent professionsComponent = entity.getComponent(ProfessionsComponent.class);
		Profession primaryProfession = professionsComponent.getPrimaryProfession(new Profession());

		entity.getPhysicalEntityComponent().setBaseAsset(assetDictionary.getMatching(humanoidBody, attributes, primaryProfession));

		// Reset other assets in entity map in case they don't match
		for (EntityAssetType type : assetMap.keySet()) {
			if (type != null) {
				assetMap.put(type, assetDictionary.getMatching(type, attributes, primaryProfession));
			}
		}

		// Then try to load saved settings
		for (Map.Entry<EntityAssetType, String> entry : typeToUniqueNameMap.entrySet()) {
			if (entry.getValue().equals(NULL_ENTITY_ASSET.getUniqueName())) {
//				assetMap.put(entry.getKey(), NULL_ENTITY_ASSET);
			} else {
				CreatureEntityAsset assetFromDictionary = assetDictionary.getByUniqueName(entry.getValue());
				if (assetFromDictionary.matches(attributes, primaryProfession)) {
					assetMap.put(assetFromDictionary.getType(), assetFromDictionary);
				} // else doesn't match, so leave as is
			}
		}

	}

	public void update(EntityAssetType assetType, CreatureEntityAsset selectedAsset) {
		typeToUniqueNameMap.put(assetType, selectedAsset.getUniqueName());
		persist();
	}

	private void persist() {
		if (isPersistable) {
			try {
				String jsonData = mapper.writeValueAsString(this);
				FileUtils.write(settingsFile.file(), jsonData);
			} catch (IOException e) {
				Logger.error("Error while writing " + this.getClass().toString() + " to file, " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
		persist();
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
		persist();
	}

	public CreatureBodyType getBodyType() {
		return bodyType;
	}

	public void setBodyType(CreatureBodyType bodyType) {
		this.bodyType = bodyType;
		persist();
	}

	public Map<EntityAssetType, String> getTypeToUniqueNameMap() {
		return typeToUniqueNameMap;
	}

	public void setTypeToUniqueNameMap(Map<EntityAssetType, String> typeToUniqueNameMap) {
		this.typeToUniqueNameMap = typeToUniqueNameMap;
	}

	public void setSkinColor(Color skinColor) {
		this.skinColor = skinColor;
		persist();
	}

	public Color getSkinColor() {
		return skinColor;
	}

	public Color getHairColor() {
		return hairColor;
	}

	public void setHairColor(Color hairColor) {
		this.hairColor = hairColor;
		persist();
	}

	public Color getAccessoryColor() {
		return accessoryColor;
	}

	public void setAccessoryColor(Color accessoryColor) {
		this.accessoryColor = accessoryColor;
		persist();
	}
}
