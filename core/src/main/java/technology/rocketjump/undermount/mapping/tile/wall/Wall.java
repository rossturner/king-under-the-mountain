package technology.rocketjump.undermount.mapping.tile.wall;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.mapping.tile.layout.WallLayout;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class Wall implements ChildPersistable {

	private WallType wallType;
	private GameMaterial material;
	private WallLayout trueLayout;

	private WallType oreType;
	private GameMaterial oreMaterial;

	public Wall() {

	}

	public Wall(WallLayout trueLayout, WallType wallType, GameMaterial material) {
		this.trueLayout = trueLayout;
		this.wallType = wallType;
		this.material = material;
	}

	public WallLayout getTrueLayout() {
		return trueLayout;
	}

	public void setTrueLayout(WallLayout trueLayout) {
		this.trueLayout = trueLayout;
	}

	public GameMaterial getMaterial() {
		return material;
	}

	public void changeType(WallType wallType, GameMaterial material) {
		this.wallType = wallType;
		this.material = material;
	}

	public WallType getWallType() {
		return wallType;
	}

	public boolean hasOre() {
		return oreType != null;
	}

	public void changeOre(WallType oreType, GameMaterial oreMaterial) {
		this.oreType = oreType;
		this.oreMaterial = oreMaterial;
	}

	public WallType getOreType() {
		return oreType;
	}

	public GameMaterial getOreMaterial() {
		return oreMaterial;
	}

	@Override
	public String toString() {
		String result = material.getMaterialName() + " wall";
		if (hasOre()) {
			result += " with " + oreMaterial.getMaterialName();
			if (oreMaterial.getMaterialType().equals(GameMaterialType.ORE)) {
				result += " ore";
			} else {
				result += " gems";
			}
		}
		return result;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("type", wallType.getWallTypeName());
		asJson.put("material", material.getMaterialName());
		asJson.put("layout", trueLayout.getId());

		if (oreType != null) {
			asJson.put("oreType", oreType.getWallTypeName());
		}
		if (oreMaterial != null) {
			asJson.put("oreMaterial", oreMaterial.getMaterialName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.wallType = relatedStores.wallTypeDictionary.getByWallTypeName(asJson.getString("type"));
		if (this.wallType == null) {
			throw new InvalidSaveException("Could not find wall type by name " + asJson.getString("type"));
		}
		this.material = relatedStores.gameMaterialDictionary.getByName(asJson.getString("material"));
		if (this.material == null) {
			throw new InvalidSaveException("Could not find material by name " + asJson.getString("material"));
		}
		this.trueLayout = new WallLayout(asJson.getIntValue("layout"));

		String oreTypeName = asJson.getString("oreType");
		if (oreTypeName != null) {
			this.oreType = relatedStores.wallTypeDictionary.getByWallTypeName(oreTypeName);
			if (this.oreType == null) {
				throw new InvalidSaveException("Could not find ore wall type by name " + oreTypeName);
			}
		}
		String oreMaterialName = asJson.getString("oreMaterial");
		if (oreMaterialName != null) {
			this.oreMaterial = relatedStores.gameMaterialDictionary.getByName(oreMaterialName);
			if (this.oreMaterial == null) {
				throw new InvalidSaveException("Could not find materail by name " + oreMaterialName);
			}
		}
	}
}
