package technology.rocketjump.undermount.entities.model.physical.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyType;
import technology.rocketjump.undermount.assets.entities.creature.model.CreatureBodyTypeDescriptor;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.Map;
import java.util.Random;

import static technology.rocketjump.undermount.entities.model.physical.creature.Consciousness.AWAKE;
import static technology.rocketjump.undermount.entities.model.physical.creature.Sanity.SANE;

public class CreatureEntityAttributes implements EntityAttributes {

	public static Color DEFAULT_BONE_COLOR = HexColors.get("#fbfbf9");

	private long seed;
	private Race race;
	private Gender gender;
	private float strength; // might want to move this to a Stats object
	private CreatureBodyType bodyType;
	private Color skinColor;
	private Color hairColor;
	private Color eyeColor;
	private Color accessoryColor;
	private Color boneColor = DEFAULT_BONE_COLOR; // MODDING expose this
	private boolean hasHair;
	private HumanoidName name;
	private Consciousness consciousness = AWAKE;
	private Sanity sanity = SANE;

	public CreatureEntityAttributes() {

	}

	public CreatureEntityAttributes(Race race, long seed, Color hairColor, Color skinColor, Color accessoryColor) {
		this.seed = seed;
		this.race = race;
		Random random = new Random(seed);
		float strengthRange = race.getMaxStrength() - race.getMinStrength();
		strength = race.getMinStrength();
		for (int i = 0; i < 3; i++) {
			strength += random.nextFloat() * (strengthRange / 3);
		}

		for (CreatureBodyTypeDescriptor bodyTypeDescriptor : race.getBodyTypes()) {
			if (bodyTypeDescriptor.getMaxStrength() != null) {
				if (strength > bodyTypeDescriptor.getMaxStrength()) {
					continue;
				}
			}
			if (bodyTypeDescriptor.getMinStrength() != null) {
				if (strength < bodyTypeDescriptor.getMinStrength()) {
					continue;
				}
			}

			this.bodyType = bodyTypeDescriptor.getValue();
			break;
		}


		this.gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE; // MODDING expose this, may not want 50/50 male/female
		this.hairColor = hairColor;
		this.skinColor = skinColor;
		this.eyeColor = Color.BLACK;
		this.hasHair = true;
		this.accessoryColor = accessoryColor;
	}

	@Override
	public CreatureEntityAttributes clone() {
		CreatureEntityAttributes cloned = new CreatureEntityAttributes();
		cloned.seed = this.seed;
		cloned.race = this.race;
		cloned.gender = this.gender;
		cloned.bodyType = this.bodyType;
		cloned.hairColor = this.hairColor.cpy();
		cloned.skinColor = this.skinColor.cpy();
		cloned.accessoryColor = this.accessoryColor.cpy();
		cloned.hasHair = this.hasHair;
		cloned.consciousness= this.consciousness;
		cloned.sanity = this.sanity;
		return cloned;
	}

	@Override
	public Map<GameMaterialType, GameMaterial> getMaterials() {
		if (race.getFeatures().getSkin() != null && race.getFeatures().getSkin().getSkinMaterial() != null) {
			return Map.of(race.getFeatures().getSkin().getSkinMaterial().getMaterialType(), race.getFeatures().getSkin().getSkinMaterial());
		} else {
			return Map.of();
		}
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public CreatureBodyType getBodyType() {
		return bodyType;
	}

	public void setBodyType(CreatureBodyType bodyType) {
		this.bodyType = bodyType;
	}

	public void setSkinColor(Color skinColor) {
		this.skinColor = skinColor;
	}

	public void setHairColor(Color hairColor) {
		this.hairColor = hairColor;
	}

	public void setBoneColor(Color boneColor) {
		this.boneColor = boneColor;
	}

	public long getSeed() {
		return seed;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		switch (coloringLayer) {
			case EYE_COLOR:
				return eyeColor;
			case HAIR_COLOR:
				return hairColor;
			case SKIN_COLOR:
				return skinColor;
			case ACCESSORY_COLOR:
				return accessoryColor;
			case BONE_COLOR:
				return boneColor;
			default:
				return null;
		}
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Gender getGender() {
		return gender;
	}

	public void setAccessoryColor(Color accessoryColor) {
		this.accessoryColor = accessoryColor;
	}

	public boolean getHasHair() {
		return hasHair;
	}

	public void setHasHair(boolean hasHair) {
		this.hasHair = hasHair;
	}

	public void setName(HumanoidName name) {
		this.name = name;
	}

	public HumanoidName getName() {
		return name;
	}

	public Consciousness getConsciousness() {
		return consciousness;
	}

	public void setConsciousness(Consciousness consciousness) {
		this.consciousness = consciousness;
	}

	public Sanity getSanity() {
		return sanity;
	}

	public void setSanity(Sanity sanity) {
		this.sanity = sanity;
	}

	@Override
	public String toString() {
		return "HumanoidEntityAttributes{" +
				"race=" + race +
				", name=" + name +
				", consciousness=" + consciousness +
				'}';
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		asJson.put("race", race.getName());
		asJson.put("gender", gender.name());
		if (!bodyType.equals(CreatureBodyType.AVERAGE)) {
			asJson.put("bodyType", bodyType.name());
		}
		asJson.put("skinColor", HexColors.toHexString(skinColor));
		asJson.put("hairColor", HexColors.toHexString(hairColor));
		asJson.put("accessoryColor", HexColors.toHexString(accessoryColor));
		if (!boneColor.equals(DEFAULT_BONE_COLOR)) {
			asJson.put("boneColor", HexColors.toHexString(boneColor));
		}
		if (hasHair) {
			asJson.put("hasHair", true);
		}
		if (name != null) {
			JSONObject nameJson = new JSONObject(true);
			name.writeTo(nameJson, savedGameStateHolder);
			asJson.put("name", nameJson);
		}
		if (!consciousness.equals(AWAKE)) {
			asJson.put("consciousness", consciousness.name());
		}
		if (!sanity.equals(SANE)) {
			asJson.put("sanity", sanity.name());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		race = relatedStores.raceDictionary.getByName(asJson.getString("race"));
		if (race == null) {
			throw new InvalidSaveException("Could not find race with name " + asJson.getString("race"));
		}
		gender = EnumParser.getEnumValue(asJson, "gender", Gender.class, Gender.FEMALE);
		bodyType = EnumParser.getEnumValue(asJson, "bodyType", CreatureBodyType.class, CreatureBodyType.AVERAGE);
		skinColor = HexColors.get(asJson.getString("skinColor"));
		hairColor = HexColors.get(asJson.getString("hairColor"));
		accessoryColor = HexColors.get(asJson.getString("accessoryColor"));

		String boneColorHex = asJson.getString("boneColor");
		if (boneColorHex != null) {
			this.boneColor = HexColors.get(boneColorHex);
		}

		eyeColor = Color.BLACK;
		hasHair = asJson.getBooleanValue("hasHair");
		JSONObject nameJson = asJson.getJSONObject("name");
		if (nameJson != null) {
			name = new HumanoidName();
			name.readFrom(nameJson, savedGameStateHolder, relatedStores);
		}
		consciousness = EnumParser.getEnumValue(asJson, "consciousness", Consciousness.class, AWAKE);
		sanity = EnumParser.getEnumValue(asJson, "sanity", Sanity.class, SANE);
		String bodyMaterialName = asJson.getString("bodyMaterial");
		if (bodyMaterialName == null) {
			throw new InvalidSaveException("Old save format");
		}
	}
}
