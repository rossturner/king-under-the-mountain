package technology.rocketjump.undermount.entities.model.physical.humanoid.status;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.behaviour.effects.FireEffectBehaviour;
import technology.rocketjump.undermount.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.model.physical.AttachedEntity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.DeathReason;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class OnFireStatus extends StatusEffect {

	private boolean litFire;

	public OnFireStatus() {
		super(Death.class, Double.MAX_VALUE, DeathReason.BURNING);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		if (!litFire) {
			messageDispatcher.dispatchMessage(MessageType.ADD_FIRE_TO_ENTITY, parentEntity);
			litFire = true;
		}
	}

	@Override
	public void onRemoval(GameContext gameContext, MessageDispatcher messageDispatcher) {
		AttachedEntitiesComponent attachedEntitiesComponent = parentEntity.getComponent(AttachedEntitiesComponent.class);
		if (attachedEntitiesComponent != null) {
			for (AttachedEntity attachedEntity : attachedEntitiesComponent.getAttachedEntities()) {
				BehaviourComponent behaviourComponent = attachedEntity.entity.getBehaviourComponent();
				if (behaviourComponent instanceof FireEffectBehaviour) {
					((FireEffectBehaviour)behaviourComponent).setToFade();
				}
			}
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
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
