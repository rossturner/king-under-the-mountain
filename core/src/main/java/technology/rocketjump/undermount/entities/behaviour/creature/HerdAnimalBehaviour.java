package technology.rocketjump.undermount.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class HerdAnimalBehaviour extends CreatureBehaviour {

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		return null;
	}

	@Override
	protected AssignedGoal attackedByCreatureResponse(Memory attackedByCreatureMemory, GameContext gameContext) {
		AssignedGoal assignedGoal = super.attackedByCreatureResponse(attackedByCreatureMemory, gameContext);
		if (creatureGroup != null) {
			creatureGroup.getSharedMemoryComponent().add(attackedByCreatureMemory, gameContext.getGameClock());
		}
		return assignedGoal;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
	}
}
