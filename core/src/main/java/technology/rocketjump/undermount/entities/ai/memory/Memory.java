package technology.rocketjump.undermount.entities.ai.memory;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.entities.model.physical.item.AmmoType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Objects;

/**
 * This class represents a memory that happened to a sapient being at some point (may be short or long term memory)
 */
public class Memory implements ChildPersistable {

	private MemoryType type;
	private double gameTimeMemoryOccurred;
	private Double expirationTime;

	private ItemType relatedItemType;
	private GameMaterial relatedMaterial;
	private String relatedGoalName;
	private AmmoType relatedAmmoType;
	private Long relatedEntityId;

	public Memory() {

	}

	public Memory(MemoryType type, GameClock gameClock) {
		this.type = type;
		this.gameTimeMemoryOccurred = gameClock.getCurrentGameTime();
		this.expirationTime = type.shortTermMemoryDurationHours == null ? null : gameClock.getCurrentGameTime() + type.shortTermMemoryDurationHours;
	}

	public MemoryType getType() {
		return type;
	}

	public double getGameTimeMemoryOccurred() {
		return gameTimeMemoryOccurred;
	}

	public Double getExpirationTime() {
		return expirationTime;
	}

	public ItemType getRelatedItemType() {
		return relatedItemType;
	}

	public void setRelatedItemType(ItemType relatedItemType) {
		this.relatedItemType = relatedItemType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Memory memory = (Memory) o;
		return Double.compare(memory.gameTimeMemoryOccurred, gameTimeMemoryOccurred) == 0 && type == memory.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type);
	}

	public GameMaterial getRelatedMaterial() {
		return relatedMaterial;
	}

	public void setRelatedMaterial(GameMaterial relatedMaterial) {
		this.relatedMaterial = relatedMaterial;
	}

	public String getRelatedGoalName() {
		return relatedGoalName;
	}

	public void setRelatedGoalName(String relatedGoalName) {
		this.relatedGoalName = relatedGoalName;
	}

	public AmmoType getRelatedAmmoType() {
		return relatedAmmoType;
	}

	public void setRelatedAmmoType(AmmoType relatedAmmoType) {
		this.relatedAmmoType = relatedAmmoType;
	}

	public void setRelatedEntityId(Long relatedEntityId) {
		this.relatedEntityId = relatedEntityId;
	}

	public Long getRelatedEntityId() {
		return relatedEntityId;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("type", type.name());
		asJson.put("occurred", gameTimeMemoryOccurred);
		if (expirationTime != null) {
			asJson.put("expiration", expirationTime);
		}

		if (relatedItemType != null) {
			asJson.put("relatedItemType", relatedItemType.getItemTypeName());
		}
		if (relatedAmmoType != null) {
			asJson.put("relatedAmmoType", relatedAmmoType.name());
		}
		if (relatedMaterial != null) {
			asJson.put("relatedMaterial", relatedMaterial.getMaterialName());
		}
		if (relatedGoalName != null) {
			asJson.put("relatedGoalName", relatedGoalName);
		}
		if (relatedEntityId != null) {
			asJson.put("relatedEntityId", relatedEntityId);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.type = EnumParser.getEnumValue(asJson, "type", MemoryType.class, null);
		this.gameTimeMemoryOccurred = asJson.getDoubleValue("occurred");
		this.expirationTime = asJson.getDouble("expiration");

		String relatedItemTypeName = asJson.getString("relatedItemType");
		if (relatedItemTypeName != null) {
			this.relatedItemType = relatedStores.itemTypeDictionary.getByName(relatedItemTypeName);
			if (this.relatedItemType == null) {
				throw new InvalidSaveException("Could not find item type by name " + relatedItemTypeName);
			}
		}

		String relatedAmmoTypeName = asJson.getString("relatedAmmoType");
		if (relatedAmmoTypeName != null) {
			this.relatedAmmoType = EnumUtils.getEnum(AmmoType.class, relatedAmmoTypeName);
			if (this.relatedAmmoType == null) {
				throw new InvalidSaveException("Could not find ammo type by name " + relatedAmmoTypeName);
			}
		}

		String relatedMaterialName = asJson.getString("relatedMaterial");
		if (relatedMaterialName != null) {
			this.relatedMaterial = relatedStores.gameMaterialDictionary.getByName(relatedMaterialName);
			if (this.relatedMaterial == null) {
				throw new InvalidSaveException("Could not find material by name " + relatedMaterialName);
			}
		}

		this.relatedEntityId = asJson.getLong("relatedEntityId");

		this.relatedGoalName = asJson.getString("relatedGoalName");
	}

}
