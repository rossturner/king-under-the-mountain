package technology.rocketjump.undermount.entities.ai.goap.actions;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.nourishment.ConsumeLiquidFromContainerAction;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.status.OnFireStatus;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundAssetMessage;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;

public class DouseSelfAction extends ConsumeLiquidFromContainerAction {

	public DouseSelfAction(AssignedGoal parent) {
		super(parent);
	}

	protected float getTimeToSpendDrinking() {
		return 1.5f;
	}

	@Override
	protected void effectsOfDrinkConsumption(GameMaterial consumedLiquid, LiquidAllocation liquidAllocation, GameContext gameContext) {
		if (consumedLiquid.isQuenchesThirst()) {
			StatusComponent statusComponent = parent.parentEntity.getComponent(StatusComponent.class);
			statusComponent.remove(OnFireStatus.class);

			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND_ASSET, new RequestSoundAssetMessage("WaterSizzle", (soundAsset) -> {
				if (soundAsset != null) {
					parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(soundAsset, parent.parentEntity));
				}
			}));
		} else {
			Logger.error(this.getSimpleName() + " does not work with " + consumedLiquid.getMaterialName() + " as it does not quench thirst");
		}
	}

}