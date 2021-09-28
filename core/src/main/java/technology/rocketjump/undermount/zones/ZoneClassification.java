package technology.rocketjump.undermount.zones;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class ZoneClassification implements ChildPersistable {

	private ZoneType zoneType;
	private boolean isConstructed;
	private GameMaterial targetMaterial;
	private boolean isHighCapacity;

	public ZoneClassification(ZoneType zoneType, boolean isConstructed, GameMaterial targetMaterial, boolean isHighCapacity) {
		this.zoneType = zoneType;
		this.isConstructed = isConstructed;
		this.targetMaterial = targetMaterial;
		this.isHighCapacity = isHighCapacity;
	}

	public ZoneClassification() {

	}

	public ZoneType getZoneType() {
		return zoneType;
	}

	public boolean isConstructed() {
		return isConstructed;
	}

	public GameMaterial getTargetMaterial() {
		return targetMaterial;
	}

	public boolean isHighCapacity() {
		return isHighCapacity;
	}

	@Override
	public String toString() {
		return zoneType + ", isConstructed=" + isConstructed + ", material=" + targetMaterial;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("type", zoneType.name());
		if (isConstructed) {
			asJson.put("isConstructed", true);
		}
		if (targetMaterial != null) {
			asJson.put("targetMaterial", targetMaterial.getMaterialName());
		}
		if (isHighCapacity) {
			asJson.put("highCapacity", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.zoneType = EnumParser.getEnumValue(asJson, "type", ZoneType.class, ZoneType.LIQUID_SOURCE);
		this.isConstructed = asJson.getBooleanValue("isConstructed");
		this.isHighCapacity = asJson.getBooleanValue("highCapacity");

		String targetMaterialName = asJson.getString("targetMaterial");
		if (targetMaterialName != null) {
			this.targetMaterial = relatedStores.gameMaterialDictionary.getByName(targetMaterialName);
			if (this.targetMaterial == null) {
				throw new InvalidSaveException("Could not find material by name " + targetMaterialName);
			}
		}
	}

	public void setTargetMaterial(GameMaterial targetLiquidMaterial) {
		this.targetMaterial = targetLiquidMaterial;
	}

	public enum ZoneType {
		LIQUID_SOURCE
	}



}
