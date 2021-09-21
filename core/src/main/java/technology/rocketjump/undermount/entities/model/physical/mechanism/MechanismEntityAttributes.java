package technology.rocketjump.undermount.entities.model.physical.mechanism;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.mapping.tile.underground.PipeLayout;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.EnumMap;
import java.util.Map;

public class MechanismEntityAttributes implements EntityAttributes {

	protected long seed;
	protected EnumMap<GameMaterialType, GameMaterial> materials = new EnumMap<>(GameMaterialType.class);
	private EnumMap<ColoringLayer, Color> otherColors = new EnumMap<>(ColoringLayer.class); // Others such as plant_branches
//	protected Color accessoryColor;

	private MechanismType mechanismType;
	private PipeLayout pipeLayout;

	public MechanismEntityAttributes() {

	}

	public MechanismEntityAttributes(long seed) {
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
		MechanismEntityAttributes cloned = new MechanismEntityAttributes(seed);
		cloned.materials.putAll(this.materials);
		cloned.otherColors.putAll(this.otherColors);
		cloned.mechanismType = this.mechanismType;
		cloned.pipeLayout = this.pipeLayout;
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

	public MechanismType getMechanismType() {
		return mechanismType;
	}

	public void setMechanismType(MechanismType mechanismType) {
		this.mechanismType = mechanismType;
		if (mechanismType == null) {
			this.pipeLayout = null;
		} else {
			// TODO only do this for pipe types
			this.pipeLayout = new PipeLayout(0);
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
		return mechanismType.getPrimaryMaterialType();
	}

	public PipeLayout getPipeLayout() {
		return pipeLayout;
	}

	public void setPipeLayout(PipeLayout pipeLayout) {
		this.pipeLayout = pipeLayout;
	}

	public GameMaterial getPrimaryMaterial() {
		if (materials.containsKey(getPrimaryMaterialType())) {
			return this.materials.get(getPrimaryMaterialType());
		} else {
			return this.materials.values().iterator().next();
		}
	}

	@Override
	public String toString() {
		return "MechanismEntityAttributes{" +
				"mechanismType=" + mechanismType +
				", pipeLayout=" + pipeLayout.getId() +
				'}';
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		JSONArray materialsJson = new JSONArray();
		for (GameMaterial material : materials.values()) {
			materialsJson.add(material.getMaterialName());
		}
		asJson.put("materials", materialsJson);
		if (!otherColors.isEmpty()) {
			JSONObject otherColorsJson = new JSONObject(true);
			for (Map.Entry<ColoringLayer, Color> entry : otherColors.entrySet()) {
				otherColorsJson.put(entry.getKey().name(), HexColors.toHexString(entry.getValue()));
			}
			asJson.put("otherColors", otherColorsJson);
		}
		if (mechanismType != null) {
			asJson.put("mechanismType", mechanismType.getName());
		}
		if (pipeLayout != null) {
			asJson.put("pipeLayout", pipeLayout.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		JSONArray materialsJson = asJson.getJSONArray("materials");
		for (int cursor = 0; cursor < materialsJson.size(); cursor++) {
			GameMaterial material = relatedStores.gameMaterialDictionary.getByName(materialsJson.getString(cursor));
			if (material == null) {
				throw new InvalidSaveException("Could not find material by name " + materialsJson.getString(cursor));
			} else {
				materials.put(material.getMaterialType(), material);
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
		String mechanismTypeName = asJson.getString("mechanismType");
		if (mechanismTypeName != null) {
			mechanismType = relatedStores.mechanismTypeDictionary.getByName(mechanismTypeName);
			if (mechanismType == null) {
				throw new InvalidSaveException("Could not find mechanism type by name " + mechanismTypeName);
			}
		}
		Integer pipeLayoutId = asJson.getInteger("pipeLayout");
		if (pipeLayoutId != null) {
			this.pipeLayout = new PipeLayout(pipeLayoutId);
		}
	}

}
