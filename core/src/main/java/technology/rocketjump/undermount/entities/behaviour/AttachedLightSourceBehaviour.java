package technology.rocketjump.undermount.entities.behaviour;

import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class AttachedLightSourceBehaviour {

	public static void infrequentUpdate(GameContext gameContext, Entity parentEntity) {
		AttachedLightSourceComponent attachedLightSourceComponent = parentEntity.getComponent(AttachedLightSourceComponent.class);
		if (attachedLightSourceComponent != null) {

		}
	}
}
