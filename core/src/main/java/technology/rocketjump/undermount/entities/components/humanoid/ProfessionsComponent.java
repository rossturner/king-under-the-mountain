package technology.rocketjump.undermount.entities.components.humanoid;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Sets;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.*;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

public class ProfessionsComponent implements EntityComponent {

	public static final int MAX_PROFESSIONS = 3;
	private Map<Profession, Float> activeProfessions = new HashMap<>();
	private Map<Profession, Float> inactiveProfessions = new HashMap<>();

	public ProfessionsComponent() {
		activeProfessions.put(NULL_PROFESSION, 0f); // NULL_PROFESSION acts as default "Villager" profession
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ProfessionsComponent cloned = new ProfessionsComponent();
		for (Map.Entry<Profession, Float> entry : this.activeProfessions.entrySet()) {
			cloned.activeProfessions.put(entry.getKey(), entry.getValue());
		}

		return cloned;
	}

	public void activate(Profession profession) {
		Float skillLevel = inactiveProfessions.remove(profession);
		if (skillLevel != null) {
			activeProfessions.put(profession, skillLevel);
		}
	}

	public void deactivate(Profession profession) {
		if (profession.equals(NULL_PROFESSION)) {
			throw new IllegalArgumentException("Can not deactivate " + NULL_PROFESSION.getName());
		}
		Float skillLevel = activeProfessions.remove(profession);
		if (skillLevel != null) {
			inactiveProfessions.put(profession, skillLevel);
		}
	}

	public boolean hasActiveProfession(Profession profession) {
		return activeProfessions.containsKey(profession);
	}

	public boolean hasInactiveProfession(Profession profession) {
		return inactiveProfessions.containsKey(profession);
	}

	public boolean hasAnyActiveProfession(Set<Profession> professionSet) {
		return !Sets.intersection(professionSet, activeProfessions.keySet()).isEmpty();
	}

	public void add(Profession profession, float skillLevel) {
		activeProfessions.put(profession, skillLevel);
	}

	public List<QuantifiedProfession> getActiveProfessions() {
		List<QuantifiedProfession> quantifiedProfessions = new ArrayList<>();
		for (Map.Entry<Profession, Float> entry : activeProfessions.entrySet()) {
			quantifiedProfessions.add(new QuantifiedProfession(entry.getKey(), entry.getValue()));
		}
		quantifiedProfessions.sort((o1, o2) -> (int)((o2.skillLevel * 10000f) - (o1.skillLevel * 10000f)));
		return quantifiedProfessions;
	}

	public Profession getPrimaryProfession(Profession defaultProfession) {
		List<QuantifiedProfession> quantifiedProfessions = getActiveProfessions();
		QuantifiedProfession highestProfession = quantifiedProfessions.get(0);
		if (highestProfession.profession.equals(NULL_PROFESSION)) {
			return defaultProfession;
		} else {
			return highestProfession.profession;
		}
	}

	public float getSkillLevel(Profession profession) {
		Float skillLevel = activeProfessions.get(profession);
		if (skillLevel == null || skillLevel <= 0) {
			return 0.2f; // Assume low skill level so unskilled jobs e.g. clearContextRelatedState plants can be completed
		} else {
			return skillLevel;
		}
	}

	public void clear() {
		activeProfessions.clear();
		activeProfessions.put(NULL_PROFESSION, 0f);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONObject activeProfessionsJson = new JSONObject(true);
		for (Map.Entry<Profession, Float> entry : activeProfessions.entrySet()) {
			if (entry.getKey().equals(NULL_PROFESSION)) {
				continue;
			}
			activeProfessionsJson.put(entry.getKey().getName(), entry.getValue());
		}
		asJson.put("active", activeProfessionsJson);


		JSONObject inactiveProfessionsJson = new JSONObject(true);
		for (Map.Entry<Profession, Float> entry : inactiveProfessions.entrySet()) {
			inactiveProfessionsJson.put(entry.getKey().getName(), entry.getValue());
		}
		asJson.put("inactive", inactiveProfessionsJson);
	}


	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject activeProfessionsJson = asJson.getJSONObject("active");
		JSONObject inactiveProfessionsJson = asJson.getJSONObject("inactive");
		if (activeProfessionsJson == null || inactiveProfessionsJson == null) {
			throw new InvalidSaveException("Unrecognised professions json");
		}

		for (String professionName: activeProfessionsJson.keySet()) {
			Profession profession = relatedStores.professionDictionary.getByName(professionName);
			if (profession == null) {
				throw new InvalidSaveException("Could not find profession with name " + professionName);
			}
			Float skillLevel = activeProfessionsJson.getFloatValue(professionName);
			activeProfessions.put(profession, skillLevel);
		}

		for (String professionName: inactiveProfessionsJson.keySet()) {
			Profession profession = relatedStores.professionDictionary.getByName(professionName);
			if (profession == null) {
				throw new InvalidSaveException("Could not find profession with name " + professionName);
			}
			Float skillLevel = inactiveProfessionsJson.getFloatValue(professionName);
			inactiveProfessions.put(profession, skillLevel);
		}
	}


    public static class QuantifiedProfession {

		private final Profession profession;
		private float skillLevel;

		public QuantifiedProfession(Profession profession, float skillLevel) {
			this.profession = profession;
			this.skillLevel = skillLevel;
		}

		public Profession getProfession() {
			return profession;
		}

		public float getSkillLevel() {
			return skillLevel;
		}

		@Override
		public String toString() {
			return "profession=" + profession + ", skillLevel=" + skillLevel;
		}
	}
}
