package technology.rocketjump.undermount.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartDamageLevel;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class MovementImpaired extends StatusEffect {

	// Lasts permanently until removed
	public MovementImpaired() {
		super(null, null, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		SteeringComponent steeringComponent = parentEntity.getBehaviourComponent().getSteeringComponent();
		steeringComponent.setMovementImpaired(true);
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		boolean noSignificantDamage = attributes.getBody().getAllDamage()
				.stream().noneMatch(d -> d.getDamageLevel().equals(BodyPartDamageLevel.BrokenBones) || d.getDamageLevel().equals(BodyPartDamageLevel.Destroyed));

		if (noSignificantDamage) {
			parentEntity.getBehaviourComponent().getSteeringComponent().setMovementImpaired(false);
		}
		return noSignificantDamage;
	}

	@Override
	public String getI18Key() {
		return "STATUS.MOVEMENT_IMPAIRED";
	}

}
