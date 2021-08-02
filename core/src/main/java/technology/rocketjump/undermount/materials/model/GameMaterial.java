package technology.rocketjump.undermount.materials.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.*;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.mapgen.model.RockGroup;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.misc.SequentialId;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.i18n.I18nString;
import technology.rocketjump.undermount.ui.i18n.I18nWord;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameMaterial implements Comparable<GameMaterial>, Persistable {

	@Name
	private String materialName;
	@SequentialId
	private long materialId;
	private GameMaterialType materialType;

	private String colorCode;
	@JsonIgnore
	private Color color;

	// Rock only properties
	private RockGroup rockGroup;
	private Float prevalence;
	private List<String> oreNames;

	private boolean alcoholic;
	private boolean combustible;
	private boolean edible;
	private boolean poisonous;
	private boolean quenchesThirst;

	// Liquid-only properties
	private Set<GameMaterial> constituentMaterials;

	private String i18nKey;
	private I18nString i18nValue; // If this is set, use it over dynamic i18nKey // FIXME this is always loaded as an i18nWord and will lose i18nText info
	private String dynamicMaterialId;
	private boolean useMaterialTypeAsAdjective; // Used for not-fully implemented materials that aren't craftable and not translated
	private boolean useInRandomGeneration = true;

	private MaterialOxidisation oxidisation;

	public static final GameMaterial NULL_MATERIAL = new GameMaterial("null-material", -1, GameMaterialType.OTHER, "#FF00FF", null, 0f, RockGroup.None,
			false, false,false, false, false, null);

	// Empty constructor for initialising from saved game
	public GameMaterial() {

	}


	// Simple constructor for testing
	public GameMaterial(String materialName, long materialId, GameMaterialType type) {
		this(materialName, materialId, type, null, null, null, null, false, false, false, false, false, null);
	}

	// TODO try to remove usage of this
	public static GameMaterial nullMaterialWithType(GameMaterialType gameMaterialType) {
		return new GameMaterial(NULL_MATERIAL.getMaterialName(), NULL_MATERIAL.getMaterialId(), gameMaterialType);
	}

	// Constructor for dynamically-created combined materials
	public GameMaterial(String dynamicMaterialId, String materialName, long materialId, GameMaterialType type,
						Color color, boolean alcoholic, boolean combustible, boolean poisonous, boolean edible, boolean quenchesThirst, Set<GameMaterial> constituentMaterials) {
		this.materialName = materialName;
		this.materialId = materialId;
		this.materialType = type;
		this.colorCode = null;
		this.color = color;
		this.rockGroup = null;
		this.prevalence = null;
		this.oreNames = null;
		this.alcoholic = alcoholic;
		this.combustible = combustible;
		this.poisonous = poisonous;
		this.edible = edible;
		this.quenchesThirst = quenchesThirst;
		this.constituentMaterials = constituentMaterials;
		this.dynamicMaterialId = dynamicMaterialId;
	}

	@JsonCreator
	public GameMaterial(@JsonProperty("materialName") String materialName, @JsonProperty("materialId") long materialId,
						@JsonProperty("materialType") GameMaterialType materialType, @JsonProperty("colorCode") String colorCode,
						@JsonProperty("oreNames") List<String> oreNames, @JsonProperty("prevalence") Float prevalence, @JsonProperty("rockGroup") RockGroup rockGroup,
						@JsonProperty("alcoholic") boolean alcoholic,
						@JsonProperty("combustible") boolean combustible,
						@JsonProperty("edible") boolean edible, @JsonProperty("poisonous") boolean poisonous,
						@JsonProperty("quenchesThirst") boolean quenchesThirst,
						@JsonProperty("oxidisation") MaterialOxidisation oxidisation) {
		this.materialName = materialName;
		this.materialId = materialId;
		this.colorCode = colorCode;
		this.rockGroup = rockGroup;
		this.prevalence = prevalence;
		this.oreNames = oreNames;
		this.alcoholic = alcoholic;
		this.combustible = combustible;
		this.edible = edible;
		this.poisonous = poisonous;
		this.quenchesThirst = quenchesThirst;
		this.constituentMaterials = null;
		this.oxidisation = oxidisation;

		if (materialType != null) {
			this.materialType = materialType;
		} else {
			this.materialType = GameMaterialType.OTHER;
		}

		if (colorCode == null) {
			color = null;
		} else if (colorCode.length() == 7) {
			color = new Color(
					Integer.parseInt(colorCode.substring(1, 3), 16) / 255f,
					Integer.parseInt(colorCode.substring(3, 5), 16) / 255f,
					Integer.parseInt(colorCode.substring(5, 7), 16) / 255f,
					1f);
		} else {
			color = null;
			Logger.error("Unrecognised color code " + colorCode + " for GameMaterial " + this.materialName);
		}
	}

	public void setMaterialId(long materialId) {
		this.materialId = materialId;
	}

	public String getMaterialName() {
		return materialName;
	}

	public long getMaterialId() {
		return materialId;
	}

	public GameMaterialType getMaterialType() {
		return materialType;
	}

	public RockGroup getRockGroup() {
		return rockGroup;
	}

	public String getColorCode() {
		return colorCode;
	}

	public Color getColor() {
		return color;
	}

	public Float getPrevalence() {
		return prevalence;
	}

	public List<String> getOreNames() {
		return oreNames;
	}

	public boolean isAlcoholic() {
		return alcoholic;
	}

	public boolean isCombustible() {
		return combustible;
	}

	public boolean isEdible() {
		return edible;
	}

	public boolean isQuenchesThirst() {
		return quenchesThirst;
	}

	public Set<GameMaterial> getConstituentMaterials() {
		return constituentMaterials;
	}

	public I18nString getI18nValue() {
		return i18nValue;
	}

	public String getI18nKey() {
		if (this.i18nKey == null) { // FIXME probably better to always explicitly set i18nKey rather than derive it like this
			return materialType.name() + "." + materialName.toUpperCase().replaceAll(" ", "_");
		} else {
			return i18nKey;
		}
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public void setI18nValue(I18nString i18nValue) {
		this.i18nValue = i18nValue;
	}

	public String getDynamicMaterialId() {
		return dynamicMaterialId;
	}

	public boolean isUseMaterialTypeAsAdjective() {
		return useMaterialTypeAsAdjective;
	}

	public void setUseMaterialTypeAsAdjective(boolean useMaterialTypeAsAdjective) {
		this.useMaterialTypeAsAdjective = useMaterialTypeAsAdjective;
	}

	public boolean isPoisonous() {
		return poisonous;
	}

	public MaterialOxidisation getOxidisation() {
		return oxidisation;
	}

	@Override
	public String toString() {
		return materialName + "(" + materialType.name() + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GameMaterial material = (GameMaterial) o;
		return materialId == material.materialId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(materialId);
	}

	@Override
	public int compareTo(GameMaterial o) {
		return this.materialName.compareTo(o.materialName);
	}

	public boolean isDynamicallyCreated() {
		return dynamicMaterialId != null;
	}

	public boolean isUseInRandomGeneration() {
		return useInRandomGeneration;
	}

	public void setUseInRandomGeneration(boolean useInRandomGeneration) {
		this.useInRandomGeneration = useInRandomGeneration;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (dynamicMaterialId == null || savedGameStateHolder.dynamicMaterials.containsKey(this.dynamicMaterialId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);

		asJson.put("dynamicId", dynamicMaterialId);
		asJson.put("name", materialName);
		asJson.put("id", materialId);
		if (!GameMaterialType.OTHER.equals(materialType)) {
			asJson.put("type", materialType.name());
		}
		if (colorCode != null) {
			asJson.put("color", colorCode);
		} else if (color != null) {
			asJson.put("color", HexColors.toHexString(color));
		}

		if (rockGroup != null) {
			asJson.put("rockGroup", rockGroup.name());
		}
		if (prevalence != null) {
			asJson.put("prevalance", prevalence);
		}
		if (oreNames != null && !oreNames.isEmpty()) {
			JSONArray namesArray = new JSONArray();
			namesArray.addAll(oreNames);
			asJson.put("oreNames", new JSONArray(namesArray));
		}

		if (alcoholic) {
			asJson.put("alcoholic", true);
		}
		if (combustible) {
			asJson.put("combustible", true);
		}
		if (edible) {
			asJson.put("edible", true);
		}
		if (poisonous) {
			asJson.put("poisonous", true);
		}
		if (constituentMaterials != null && !constituentMaterials.isEmpty()) {
			JSONArray constituentsArray = new JSONArray();
			for (GameMaterial constituentMaterial : constituentMaterials) {
				constituentMaterial.writeTo(savedGameStateHolder);
				constituentsArray.add(constituentMaterial.getMaterialId());
			}
			asJson.put("constituentMaterialIds", constituentsArray);
		}

		if (i18nKey != null) {
			asJson.put("i18nKey", i18nKey);
		}
		if (i18nValue != null) {
			asJson.put("i18nValue", i18nValue.toString());
		}
		if (useMaterialTypeAsAdjective) {
			asJson.put("useMaterialTypeAsAdjective", true);
		}
		if (useInRandomGeneration) {
			asJson.put("useInRandomGeneration", true);
		}

		savedGameStateHolder.dynamicMaterials.put(dynamicMaterialId, this);
		savedGameStateHolder.dynamicMaterialsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder,
						 SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.dynamicMaterialId = asJson.getString("dynamicId");
		this.materialName = asJson.getString("name");
		this.materialId = asJson.getLongValue("id");

		String materialTypeName = asJson.getString("type");
		if (materialTypeName == null) {
			this.materialType = GameMaterialType.OTHER;
		} else {
			try {
				this.materialType = GameMaterialType.valueOf(materialTypeName);
			} catch (IllegalArgumentException e) {
				throw new InvalidSaveException("Unrecognised material type: " + materialTypeName);
			}
		}

		colorCode = asJson.getString("color");
		if (colorCode != null) {
			color = HexColors.get(colorCode);
		}

		String rockGroupName = asJson.getString("rockGroup");
		if (rockGroupName != null) {
			try {
				rockGroup = RockGroup.valueOf(rockGroupName);
			} catch (IllegalArgumentException e) {
				throw new InvalidSaveException("Unrecognised rock group: " + materialTypeName);
			}
		}

		Float prevalence = asJson.getFloat("prevalence");
		if (prevalence != null) {
			this.prevalence = prevalence;
		}

		JSONArray oreNames = asJson.getJSONArray("oreNames");
		if (oreNames != null) {
			this.oreNames = new ArrayList<>();
			for (Object oreNameObj : oreNames) {
				this.oreNames.add((String) oreNameObj);
			}
		}

		this.alcoholic = asJson.getBooleanValue("alcoholic");
		this.combustible = asJson.getBooleanValue("combustible");
		this.edible = asJson.getBooleanValue("edible");
		this.poisonous = asJson.getBooleanValue("poisonous");

		JSONArray constituentMaterialIds = asJson.getJSONArray("constituentMaterialIds");
		if (constituentMaterialIds != null) {
			this.constituentMaterials = new TreeSet<>();
			for (int cursor = 0; cursor < constituentMaterialIds.size(); cursor++) {
				long constituentMaterialId = constituentMaterialIds.getLongValue(cursor);
				GameMaterial material = relatedStores.gameMaterialDictionary.getById(constituentMaterialId);
				if (material != null) {
					constituentMaterials.add(material);
				} else {
					Logger.error("Could not find material by ID " + constituentMaterialId + " when loading " + materialName);
				}
			}
		}

		String i18nKey = asJson.getString("i18nKey");
		if (i18nKey != null) {
			this.i18nKey = i18nKey;
		}
		String i18nValue = asJson.getString("i18nValue");
		if (i18nValue != null) {
			this.i18nValue = new I18nWord(i18nValue);
		}
		this.useMaterialTypeAsAdjective = asJson.getBooleanValue("useMaterialTypeAsAdjective");
		this.useInRandomGeneration = asJson.getBooleanValue("useInRandomGeneration");

		savedGameStateHolder.dynamicMaterials.put(dynamicMaterialId, this);
	}

}