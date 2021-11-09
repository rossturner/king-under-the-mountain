package technology.rocketjump.undermount.entities.model.physical.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.EnumMap;
import java.util.Map;

public class FurnitureEntityAttributes implements EntityAttributes {

	protected long seed;
	protected EnumMap<GameMaterialType, GameMaterial> materials = new EnumMap<>(GameMaterialType.class);
	private EnumMap<ColoringLayer, Color> otherColors = new EnumMap<>(ColoringLayer.class); // Others such as plant_branches
	protected GameMaterialType primaryMaterialType;
	protected Color accessoryColor;

	private FurnitureType furnitureType;
	private FurnitureLayout currentLayout;

	private Long assignedToEntityId;
	private int damageAmount;
	private EntityDestructionCause destructionCause;

	public FurnitureEntityAttributes() {

	}

	public FurnitureEntityAttributes(long seed) {
		this.seed = seed;
	}

	@Override
	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	@Override
	public EntityAttributes clone() {
		FurnitureEntityAttributes cloned = new FurnitureEntityAttributes(seed);
		cloned.furnitureType = this.furnitureType;
		for (Map.Entry<GameMaterialType, GameMaterial> entry : materials.entrySet()) {
			cloned.materials.put(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<ColoringLayer, Color> entry : this.otherColors.entrySet()) {
			cloned.otherColors.put(entry.getKey(), entry.getValue().cpy());
		}
		cloned.primaryMaterialType = this.primaryMaterialType;
		cloned.currentLayout = this.currentLayout;
		cloned.assignedToEntityId = this.assignedToEntityId;
		cloned.destructionCause = this.destructionCause;
		return cloned;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		GameMaterialType materialType = coloringLayer.getLinkedMaterialType();
		if (otherColors.containsKey(coloringLayer)) {
			return otherColors.get(coloringLayer);
		} else {
			GameMaterial gameMaterial = materials.get(materialType);
			if (gameMaterial != null) {
				return gameMaterial.getColor();
			} else {
				return null;
			}
		}
	}

	public EnumMap<ColoringLayer, Color> getOtherColors() {
		return otherColors;
	}

	public FurnitureType getFurnitureType() {
		return furnitureType;
	}

	public void setFurnitureType(FurnitureType furnitureType) {
		this.furnitureType = furnitureType;
		if (furnitureType == null) {
			this.currentLayout = null;
		} else {
			this.currentLayout = furnitureType.getDefaultLayout();
		}
	}

	public void setColor(ColoringLayer coloringLayer, Color color) {
		if (color != null && coloringLayer != null) {
			otherColors.put(coloringLayer, color);
		}
	}

	public EnumMap<GameMaterialType, GameMaterial> getMaterials() {
		return materials;
	}

	public void setMaterials(EnumMap<GameMaterialType, GameMaterial> materials) {
		this.materials = materials;
	}

	public void setMaterial(GameMaterial material) {
		setMaterial(material, true);
	}

	public void setMaterial(GameMaterial material, boolean overrideExisting) {
		if (material != null) {
			if (materials.containsKey(material.getMaterialType()) && !overrideExisting) {
				return;
			}
			this.materials.put(material.getMaterialType(), material);
		}
	}

	public void removeMaterial(GameMaterialType gameMaterialType) {
		materials.remove(gameMaterialType);
	}

	public GameMaterialType getPrimaryMaterialType() {
		return primaryMaterialType;
	}

	public void setPrimaryMaterialType(GameMaterialType primaryMaterialType) {
		this.primaryMaterialType = primaryMaterialType;
	}

	public FurnitureLayout getCurrentLayout() {
		return currentLayout;
	}

	public void setCurrentLayout(FurnitureLayout currentLayout) {
		this.currentLayout = currentLayout;
	}

	public GameMaterial getPrimaryMaterial() {
		if (materials.containsKey(primaryMaterialType)) {
			return this.materials.get(primaryMaterialType);
		} else {
			return this.materials.values().iterator().next();
		}
	}

	@Override
	public String toString() {
		return "FurnitureEntityAttributes{" +
				"furnitureType=" + furnitureType +
				", primaryMaterialType=" + primaryMaterialType +
				", currentLayout=" + currentLayout +
				'}';
	}

	public Long getAssignedToEntityId() {
		return assignedToEntityId;
	}

	public void setAssignedToEntityId(Long assignedToEntityId) {
		this.assignedToEntityId = assignedToEntityId;
	}

	public boolean isDestroyed() {
		return destructionCause != null;
	}

	public int getDamageAmount() {
		return damageAmount;
	}

	public void setDamageAmount(int damageAmount) {
		this.damageAmount = damageAmount;
	}

	public void setDestroyed(EntityDestructionCause cause) {
		this.destructionCause = cause;
	}

	public EntityDestructionCause getDestructionCause() {
		return destructionCause;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		JSONObject materialsJson = new JSONObject();
		for (GameMaterial material : materials.values()) {
			materialsJson.put(material.getMaterialType().name(), material.getMaterialName());
		}
		asJson.put("materials", materialsJson);
		if (!otherColors.isEmpty()) {
			JSONObject otherColorsJson = new JSONObject(true);
			for (Map.Entry<ColoringLayer, Color> entry : otherColors.entrySet()) {
				otherColorsJson.put(entry.getKey().name(), HexColors.toHexString(entry.getValue()));
			}
			asJson.put("otherColors", otherColorsJson);
		}
		asJson.put("primaryMaterialType", primaryMaterialType.name());
		if (accessoryColor != null) {
			asJson.put("accessoryColor", HexColors.toHexString(accessoryColor));
		}
		if (furnitureType != null) {
			asJson.put("furnitureType", furnitureType.getName());
		}
		if (currentLayout != null) {
			asJson.put("layout", currentLayout.getUniqueName());
		}
		if (assignedToEntityId != null) {
			asJson.put("assignedToEntityId", assignedToEntityId);
		}
		if (damageAmount > 0) {
			asJson.put("damage", damageAmount);
		}
		if (destructionCause != null) {
			asJson.put("destructionCause", this.destructionCause);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		if (asJson.get("materials") instanceof JSONArray) {
			throw new InvalidSaveException("Old save file structure");
		}
		JSONObject materialsJson = asJson.getJSONObject("materials");
		for (Map.Entry<String, Object> entry : materialsJson.entrySet()) {
			GameMaterial material = relatedStores.gameMaterialDictionary.getByName(entry.getValue().toString());
			if (material == null) {
				throw new InvalidSaveException("Could not find material by name " + entry.getValue().toString());
			} else {
				materials.put(GameMaterialType.valueOf(entry.getKey()), material);
			}
		}
		JSONObject otherColorsJson = asJson.getJSONObject("otherColors");
		if (otherColorsJson != null) {
			for (String coloringLayerName : otherColorsJson.keySet()) {
				ColoringLayer coloringLayer = EnumUtils.getEnum(ColoringLayer.class, coloringLayerName);
				if (coloringLayer == null) {
					throw new InvalidSaveException("Could not find coloring layer by name " + coloringLayerName);
				}
				Color color = HexColors.get(otherColorsJson.getString(coloringLayerName));
				otherColors.put(coloringLayer, color);
			}
		}
		primaryMaterialType = EnumParser.getEnumValue(asJson, "primaryMaterialType", GameMaterialType.class, null);
		if (primaryMaterialType == null) {
			throw new InvalidSaveException("Could not find material type by name " + asJson.getString("primaryMaterialType"));
		}

		String accessoryColor = asJson.getString("accessoryColor");
		if (accessoryColor != null) {
			this.accessoryColor = HexColors.get(accessoryColor);
		}
		String furnitureTypeName = asJson.getString("furnitureType");
		if (furnitureTypeName != null) {
			furnitureType = relatedStores.furnitureTypeDictionary.getByName(furnitureTypeName);
			if (furnitureType == null) {
				throw new InvalidSaveException("Could not find furniture type by name " + furnitureTypeName);
			}
		}
		String layoutName = asJson.getString("layout");
		if (layoutName != null) {
			currentLayout = relatedStores.furnitureLayoutDictionary.getByName(layoutName);
			if (currentLayout == null) {
				throw new InvalidSaveException("Could not find furniture layout by name " + layoutName);
			}
		}
		assignedToEntityId = asJson.getLong("assignedToEntityId");

		damageAmount = asJson.getIntValue("damage");
		destructionCause = EnumParser.getEnumValue(asJson, "destructionCause", EntityDestructionCause.class, null);
	}

}
