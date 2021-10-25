package technology.rocketjump.undermount.entities.model.physical.creature.status;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.StatusMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public abstract class StatusEffect implements ChildPersistable {

	protected Entity parentEntity;
	protected double timeApplied = 0;

	protected final Class<? extends StatusEffect> nextStage;
	protected final Double hoursUntilNextStage;
	protected final DeathReason deathReason; // Only used if this statuseffect applies death

	protected StatusEffect(Class<? extends StatusEffect> nextStage, Double hoursUntilNextStage, DeathReason deathReason) {
		this.nextStage = nextStage;
		this.hoursUntilNextStage = hoursUntilNextStage;
		this.deathReason = deathReason;
	}

	public void infrequentUpdate(double elapsedTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
		timeApplied += elapsedTime;

		if (checkForRemoval(gameContext)) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_STATUS, new StatusMessage(parentEntity, this.getClass(), null));
		} else if (nextStage != null && hoursUntilNextStage != null && timeApplied > hoursUntilNextStage) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_STATUS, new StatusMessage(parentEntity, this.getClass(), null));
			messageDispatcher.dispatchMessage(MessageType.APPLY_STATUS, new StatusMessage(parentEntity, nextStage, deathReason));
		} else {
			applyOngoingEffect(gameContext, messageDispatcher);
		}
	}


	public abstract void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher);

	public abstract boolean checkForRemoval(GameContext gameContext);

	public void onRemoval(GameContext gameContext, MessageDispatcher messageDispatcher) {

	}

	public void setParentEntity(Entity parentEntity) {
		this.parentEntity = parentEntity;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("applied", timeApplied);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.timeApplied = asJson.getDoubleValue("applied");
	}
}
