package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class ParticleEffectsWhileProcessingTag extends Tag {

	@Override
	public String getTagName() {
		return "PARTICLE_EFFECTS_WHILE_PROCESSING";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		for (String arg : getArgs()) {
			if (tagProcessingUtils.particleEffectTypeDictionary.getByName(arg) == null) {
				Logger.error("Can not find particle effect with name " + arg + " for " + this.getClass().getSimpleName());
				return false;
			}
		}
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		BehaviourComponent behaviourComponent = entity.getBehaviourComponent();
		if (behaviourComponent != null) {
			if (behaviourComponent instanceof CraftingStationBehaviour) {
				CraftingStationBehaviour craftingStationBehaviour = (CraftingStationBehaviour) behaviourComponent;
				for (String arg : getArgs()) {
					craftingStationBehaviour.getParticleEffectsWhenProcessing().add(tagProcessingUtils.particleEffectTypeDictionary.getByName(arg));
				}
			} else {
				Logger.error(this.getClass().getSimpleName() + " must apply to a furniture with " + CraftingStationBehaviourTag.class.getSimpleName());
			}
		}
	}

}
