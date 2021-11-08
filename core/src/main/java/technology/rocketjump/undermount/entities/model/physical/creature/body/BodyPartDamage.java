package technology.rocketjump.undermount.entities.model.physical.creature.body;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageLevel;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartDamageLevel.Bruised;
import static technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartDamageLevel.None;
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

	public Map<BodyPartOrgan, OrganDamageLevel> getOrganDamage() {
		return organDamage;
	}

	public void healOneLevel() {
		switch (damageLevel) {
			case BrokenBones:
			case Bleeding:
				this.damageLevel = Bruised;
				break;
			case Bruised:
				this.damageLevel = None;
				break;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!damageLevel.equals(BodyPartDamageLevel.None)) {
			asJson.put("damageLevel", damageLevel.name());
		}

		if (!organDamage.isEmpty()) {
			JSONArray organDamageJson = new JSONArray();
			for (Map.Entry<BodyPartOrgan, OrganDamageLevel> entry : organDamage.entrySet()) {
				JSONObject organDamageEntryJson = new JSONObject(true);
				entry.getKey().writeTo(organDamageEntryJson, savedGameStateHolder);
				organDamageEntryJson.put("level", entry.getValue().name());
				organDamageJson.add(organDamageEntryJson);
			}
			asJson.put("organDamage", organDamageJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.damageLevel = EnumParser.getEnumValue(asJson, "damageLevel", BodyPartDamageLevel.class, BodyPartDamageLevel.None);

		JSONArray organDamageJson = asJson.getJSONArray("organDamage");
		if (organDamageJson != null) {
			for (int cursor = 0; cursor < organDamageJson.size(); cursor++) {
				JSONObject organDamageEntryJson = organDamageJson.getJSONObject(cursor);
				BodyPartOrgan bodyPartOrgan = new BodyPartOrgan();
				bodyPartOrgan.readFrom(organDamageEntryJson, savedGameStateHolder, relatedStores);
				OrganDamageLevel organDamageLevel = EnumParser.getEnumValue(organDamageEntryJson, "level", OrganDamageLevel.class, NONE);
				this.organDamage.put(bodyPartOrgan, organDamageLevel);
			}
		}
	}
}
