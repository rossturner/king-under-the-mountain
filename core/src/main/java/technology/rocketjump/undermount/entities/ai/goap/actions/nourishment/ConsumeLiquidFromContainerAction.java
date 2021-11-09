package technology.rocketjump.undermount.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.ai.memory.MemoryType;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.creature.status.alcohol.Drunk;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.ai.goap.EntityNeed.DRINK;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.components.LiquidAllocation.LiquidAllocationType.FROM_RIVER;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.DRANK_FROM_RIVER;

public class ConsumeLiquidFromContainerAction extends Action {

	private static final double AMOUNT_DRINK_NEED_RESTORED = 70.0;
	public static final float TIME_TO_SPEND_DRINKING_SECONDS = 4.5f;

	protected float elapsedTime;

	public ConsumeLiquidFromContainerAction(AssignedGoal parent) {
		super(parent);
	}

	protected float getTimeToSpendDrinking() {
		return TIME_TO_SPEND_DRINKING_SECONDS;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		LiquidAllocation liquidAllocation = parent.getLiquidAllocation();
		if (liquidAllocation == null) {
			completionType = FAILURE;
			return;
		}

		GameMaterial consumedLiquid = GameMaterial.NULL_MATERIAL;
		elapsedTime += deltaTime;
		if (elapsedTime > getTimeToSpendDrinking()) {
			// Just going to assume we're on the correct position, doesn't matter too much if we were pushed away
			MapTile targetZoneTile = gameContext.getAreaMap().getTile(liquidAllocation.getTargetZoneTile().getTargetTile());
			Entity targetFurniture = getFirstFurnitureEntity(targetZoneTile);
			if (targetFurniture != null) {
				LiquidContainerComponent liquidContainerComponent = targetFurniture.getComponent(LiquidContainerComponent.class);
				if (liquidContainerComponent != null) {
					LiquidAllocation success = liquidContainerComponent.cancelAllocationAndDecrementQuantity(liquidAllocation);
					parent.setLiquidAllocation(null);
					if (success != null) {
						consumedLiquid = liquidContainerComponent.getTargetLiquidMaterial();
						completionType = SUCCESS;
					} else {
						Logger.error("Failed to cancel liquid allocation correctly");
						completionType = FAILURE;
					}
				} else {
					Logger.error("Target furniture for " + this.getClass().getSimpleName() + " does not have " + LiquidContainerComponent.class.getSimpleName());
					completionType = FAILURE;
				}
			} else if (targetZoneTile.getFloor().isRiverTile()) {
				// No real liquid to remove
				consumedLiquid = targetZoneTile.getFloor().getMaterial();
				completionType = SUCCESS;
			} else {
				Logger.error("Not found target for " + this.getClass().getSimpleName() + ", could be removed furniture");
				completionType = FAILURE;
			}

			if (completionType.equals(SUCCESS)) {
				effectsOfDrinkConsumption(consumedLiquid, liquidAllocation, gameContext);
			}
		}
	}

	protected void effectsOfDrinkConsumption(GameMaterial consumedLiquid, LiquidAllocation liquidAllocation, GameContext gameContext) {
		if (liquidAllocation != null && FROM_RIVER.equals(liquidAllocation.getType())) {
			parent.parentEntity.getComponent(HappinessComponent.class).add(DRANK_FROM_RIVER);
		}

		if (consumedLiquid == null) {
			Logger.error("Null material consumed as liquid");
			return;
		}

		if (consumedLiquid.isQuenchesThirst()) {
			NeedsComponent needsComponent = parent.parentEntity.getComponent(NeedsComponent.class);
			needsComponent.setValue(DRINK, needsComponent.getValue(DRINK) + AMOUNT_DRINK_NEED_RESTORED);
		} else {
			Logger.warn("Consuming liquid which does not quench thirst: " + consumedLiquid.toString());
		}

		if (consumedLiquid.isAlcoholic()) {
			parent.parentEntity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.DRANK_ALCOHOL);
			parent.parentEntity.getComponent(MemoryComponent.class).addShortTerm(new Memory(MemoryType.CONSUMED_ALCOHOLIC_DRINK, gameContext.getGameClock()), gameContext.getGameClock());
			parent.parentEntity.getComponent(StatusComponent.class).apply(new Drunk());
		}

	}

	public static Entity getFirstFurnitureEntity(MapTile targetTile) {
		for (Entity entity : targetTile.getEntities()) {
			if (entity.getType().equals(EntityType.FURNITURE)) {
				return entity;
			}
		}
		return null;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("elapsed", elapsedTime);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		elapsedTime = asJson.getFloatValue("elapsed");
	}
}
