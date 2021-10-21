package technology.rocketjump.undermount.entities.model.physical.creature.body;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageLevel;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageLevel.NONE;

public class BodyPartDamage implements ChildPersistable {

	private BodyPartDamageLevel damageLevel = BodyPartDamageLevel.None;
	private Map<BodyPartOrgan, OrganDamageLevel> organDamage = new HashMap<>();

	public BodyPartDamageLevel getDamageLevel() {
		return damageLevel;
	}

	public void setDamageLevel(BodyPartDamageLevel damageLevel) {
		this.damageLevel = damageLevel;
	}

	public OrganDamageLevel getOrganDamageLevel(BodyPartOrgan organ) {
		return organDamage.getOrDefault(organ, NONE);
	}

	public void setOrganDamageLevel(BodyPartOrgan organ, OrganDamageLevel organDamageLevel) {
		this.organDamage.put(organ, organDamageLevel);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
