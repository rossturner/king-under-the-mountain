package technology.rocketjump.undermount.entities.behaviour.effects;


import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.status.OnFireStatus;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.particles.custom_libgdx.ShaderEffect;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;

import java.util.Random;

import static technology.rocketjump.undermount.entities.behaviour.effects.BaseOngoingEffectBehaviour.OngoingEffectState.ACTIVE;
import static technology.rocketjump.undermount.entities.behaviour.effects.BaseOngoingEffectBehaviour.OngoingEffectState.FADING;
import static technology.rocketjump.undermount.entities.behaviour.effects.FireEffectBehaviour.FireContinuationAction.CONTINUE_BURNING;

public class FireEffectBehaviour extends BaseOngoingEffectBehaviour {


	@Override
	public FireEffectBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		FireEffectBehaviour cloned = new FireEffectBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		super.update(deltaTime, gameContext);
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		ParticleEffectInstance particleEffectInstance = currentParticleEffect.get();

		if (particleEffectInstance != null && particleEffectInstance.getWrappedInstance() instanceof ShaderEffect) {
			ShaderEffect wrappedInstance = (ShaderEffect) particleEffectInstance.getWrappedInstance();
			if (ACTIVE.equals(state)) {
				float alpha = Math.min(stateDuration, 1f);
				wrappedInstance.getTint().a = alpha;
			} else if (FADING.equals(state)) {
				float fadeDuration = attributes.getType().getStates().get(FADING).getDuration();
				float alpha =  (fadeDuration - stateDuration) / fadeDuration;
				wrappedInstance.getTint().a = alpha;
			}
		}

	}

	@Override
	protected void nextState(GameContext gameContext) {
		switch (state) {
			case STARTING:
				this.state = ACTIVE;
				break;
			case ACTIVE:
				// Is this on a combustible tile or entity
				FireContinuationAction continuationAction = FireContinuationAction.DIE_OUT;

				MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
				if (parentTile != null) {

					for (Entity entity : parentTile.getEntities()) {
						boolean isCombustible = entity.getPhysicalEntityComponent().getAttributes().getMaterials().values()
								.stream().anyMatch(GameMaterial::isCombustible);
						if (isCombustible) {
							StatusComponent statusComponent = entity.getOrCreateComponent(StatusComponent.class);
							statusComponent.apply(new OnFireStatus());
						}
					}


					if (parentTile.hasWall()) {

						if (parentTile.getWall().getMaterial().isCombustible()) {
							continuationAction = rollForContinuation(gameContext.getRandom());
						}
					} else {
						if (parentTile.getFloor().getMaterial().isCombustible()) {
							continuationAction = rollForContinuation(gameContext.getRandom());
						}
					}

				}

				switch (continuationAction) {
					case SPREAD_TO_OTHER_TILES:
						messageDispatcher.dispatchMessage(MessageType.SPREAD_FIRE, parentTile);
						// fall through
					case CONTINUE_BURNING:
						this.state = ACTIVE;
						break;
					case CONSUME_PARENT:
						Logger.warn("TODO: consume parent");
						break;
					case DIE_OUT:
						this.state = FADING;
						break;
				}
				break;
			case FADING:
				this.state = null;
				break;
		}

		this.stateDuration = 0f;
	}


	@Override
	public void infrequentUpdate(GameContext gameContext) {
		if (parentEntity.getLocationComponent().getContainerEntity() != null) {
			// Attached to another entity
			return;
		}

		OngoingEffectAttributes attributes = (OngoingEffectAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (stateDuration > attributes.getType().getStates().get(OngoingEffectState.ACTIVE).getDuration()) {


		}
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	private FireContinuationAction rollForContinuation(Random random) {
		float roll = random.nextFloat();
		for (FireContinuationAction continuationAction : FireContinuationAction.values()) {
			if (roll <= continuationAction.chance) {
				return continuationAction;
			} else {
				roll -= continuationAction.chance;
			}
		}
		return CONTINUE_BURNING;
	}

	public enum FireContinuationAction {

		CONTINUE_BURNING(0.3f),
		SPREAD_TO_OTHER_TILES(0.3f),
		CONSUME_PARENT(0.4f),
		DIE_OUT(1f);

		private final float chance;

		FireContinuationAction(float chance) {
			this.chance = chance;
		}
	}

}
