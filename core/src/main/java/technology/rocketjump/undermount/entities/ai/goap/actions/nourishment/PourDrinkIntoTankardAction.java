package technology.rocketjump.undermount.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.cooking.model.FoodAllocation;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.actions.Action;
import technology.rocketjump.undermount.entities.ai.goap.actions.EntityCreatedCallback;
import technology.rocketjump.undermount.entities.ai.goap.actions.ItemTypeLookupCallback;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.humanoid.EquippedItemComponent;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.undermount.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.undermount.entities.ai.goap.actions.nourishment.ConsumeLiquidFromContainerAction.getFirstFurnitureEntity;
import static technology.rocketjump.undermount.entities.components.LiquidAllocation.LiquidAllocationType.FROM_LIQUID_CONTAINER;

public class PourDrinkIntoTankardAction extends Action implements EntityCreatedCallback, ItemTypeLookupCallback {

	// MODDING expose these hard-coded value
	private static final String TANKARD_ITEM_TYPE_NAME = "Ingredient-Tankard";
	private static final float TIME_TO_POUR = 2f;

	private float elapsedTime;
	private boolean pouringInProgress;

	public PourDrinkIntoTankardAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (pouringInProgress) {
			elapsedTime += deltaTime;

			if (elapsedTime > TIME_TO_POUR) {
				completionType = SUCCESS;
			}
			return;
		}


		LiquidAllocation liquidAllocation = parent.getLiquidAllocation();
		if (liquidAllocation == null || !FROM_LIQUID_CONTAINER.equals(liquidAllocation.getType()) || !liquidAllocation.getLiquidMaterial().isAlcoholic()) {
			completionType = FAILURE;
			return;
		}

		MapTile targetZoneTile = gameContext.getAreaMap().getTile(liquidAllocation.getTargetZoneTile().getTargetTile());
		Entity targetFurniture = getFirstFurnitureEntity(targetZoneTile);
		if (targetFurniture != null) {
			LiquidContainerComponent liquidContainerComponent = targetFurniture.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null) {
				GameMaterial liquidMaterial = liquidContainerComponent.getTargetLiquidMaterial();
				LiquidAllocation cancelledAllocation = liquidContainerComponent.cancelAllocationAndDecrementQuantity(liquidAllocation);
				parent.setLiquidAllocation(null);
				if (cancelledAllocation != null) {
					// correctly cancelled allocation

					Entity tankardEntity = createTankardContaining(cancelledAllocation.getAllocationAmount(), liquidMaterial, gameContext);

					if (tankardEntity != null) {
						equip(tankardEntity);
						playBeerTapperSound();
						pouringInProgress = true;
						elapsedTime += deltaTime;

					} else {
						completionType = FAILURE;
					}
				} else {
					Logger.error("Failed to cancel liquid allocation correctly");
					completionType = FAILURE;
				}
			} else {
				Logger.error("Target furniture for " + this.getClass().getSimpleName() + " does not have " + LiquidContainerComponent.class.getSimpleName());
				completionType = FAILURE;
			}
		} else {
			Logger.error("Not found target for " + this.getClass().getSimpleName() + ", could be removed furniture");
			completionType = FAILURE;
		}
	}

	private void playBeerTapperSound() {
		// MODDING expose this
		parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND_ASSET, new RequestSoundAssetMessage("BeerTap", (asset) -> {
			if (asset != null) {
				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(asset, parent.parentEntity));
			}
		}));
	}

	private Entity createTankardContaining(float liquidAmount, GameMaterial liquidMaterial, GameContext gameContext) {
		parent.messageDispatcher.dispatchMessage(MessageType.LOOKUP_ITEM_TYPE, new LookupMessage(EntityType.ITEM, TANKARD_ITEM_TYPE_NAME, this));
		if (itemTypeLookup.isEmpty()) {
			Logger.error("Could not find item type with name " + TANKARD_ITEM_TYPE_NAME);
			return null;
		}

		parent.messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(itemTypeLookup.get(), this));
		if (createdEntity == null) {
			return null;
		} else {
			LiquidContainerComponent liquidContainerComponent = new LiquidContainerComponent();
			liquidContainerComponent.init(createdEntity, parent.messageDispatcher, gameContext);
			createdEntity.addComponent(liquidContainerComponent);
			liquidContainerComponent.setTargetLiquidMaterial(liquidMaterial);
			liquidContainerComponent.setLiquidQuantity(liquidAmount);
			parent.messageDispatcher.dispatchMessage(MessageType.LIQUID_SPLASH, new LiquidSplashMessage(parent.parentEntity, liquidMaterial));
			return createdEntity;
		}
	}

	private void equip(Entity tankardEntity) {
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
		equippedItemComponent.setEquippedItem(tankardEntity, parent.parentEntity, parent.messageDispatcher);

		// Pseudo-create food allocation so PlaceFoodOrDrinkOnFurniture action works
		FoodAllocation foodAllocation = new FoodAllocation(FoodAllocation.FoodAllocationType.LIQUID_CONTAINER, tankardEntity, (LiquidAllocation) null);
		parent.setFoodAllocation(foodAllocation);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("elapsed", elapsedTime);
		if (pouringInProgress) {
			asJson.put("pouringInProgress", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		elapsedTime = asJson.getFloatValue("elapsed");
		this.pouringInProgress = asJson.getBooleanValue("pouringInProgress");
	}

	private Entity createdEntity;

	@Override
	public void entityCreated(Entity entity) {
		this.createdEntity = entity;
	}

	private Optional<ItemType> itemTypeLookup = Optional.empty();

	@Override
	public void itemTypeFound(Optional<ItemType> itemTypeLookup) {
		this.itemTypeLookup = itemTypeLookup;
	}
}
