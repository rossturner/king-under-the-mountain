package technology.rocketjump.undermount.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.undermount.entities.ai.goap.*;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.ai.memory.MemoryType;
import technology.rocketjump.undermount.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.undermount.entities.components.*;
import technology.rocketjump.undermount.entities.components.humanoid.*;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.*;
import technology.rocketjump.undermount.entities.model.physical.creature.status.Blinded;
import technology.rocketjump.undermount.entities.model.physical.creature.status.TemporaryBlinded;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.AmmoType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.util.*;

import static technology.rocketjump.undermount.entities.ItemEntityMessageHandler.findStockpileAllocation;
import static technology.rocketjump.undermount.entities.ai.goap.SpecialGoal.*;
import static technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour.getMemoryOfAttackedByCreature;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.DUE_TO_BE_HAULED;
import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.HappinessModifier.SAW_DEAD_BODY;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.MIN_HAPPINESS_VALUE;
import static technology.rocketjump.undermount.entities.model.EntityType.*;
import static technology.rocketjump.undermount.entities.model.physical.creature.Consciousness.*;
import static technology.rocketjump.undermount.environment.model.WeatherType.HappinessInteraction.STANDING;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

public class SettlerBehaviour implements BehaviourComponent, Destructible,
		RequestLiquidAllocationMessage.LiquidAllocationCallback, SelectableDescription {

	private static final float MAX_DISTANCE_TO_DOUSE_FIRE = 12f;
	private static final float AMOUNT_REQUIRED_TO_DOUSE_FIRE = 0.5f;
	private static final int MAX_TANTRUMS = 3;

	protected MessageDispatcher messageDispatcher;
	protected Entity parentEntity;
	protected GoalDictionary goalDictionary;
	protected RoomStore roomStore;

	protected AssignedGoal currentGoal;
	protected final GoalQueue goalQueue = new GoalQueue();
	protected SteeringComponent steeringComponent = new SteeringComponent();
	protected transient double lastUpdateGameTime;
	private static final int DISTANCE_TO_LOOK_AROUND = 5;
	private float stunTime;

	public void constructWith(GoalDictionary goalDictionary, RoomStore roomStore) {
		this.goalDictionary = goalDictionary;
		this.roomStore = roomStore;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		steeringComponent.init(parentEntity, gameContext.getAreaMap(), parentEntity.getLocationComponent(), messageDispatcher);

		if (currentGoal != null) {
			currentGoal.init(parentEntity, messageDispatcher);
		}
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (currentGoal != null) {
			currentGoal.destroy(parentEntity, messageDispatcher, gameContext);
			currentGoal = null;
		}
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (currentGoal == null || currentGoal.isComplete()) {
			currentGoal = pickNextGoalFromQueue(gameContext);
		}

		// Not going to update steering when asleep so can't be pushed around
		Consciousness consciousness = ((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getConsciousness();
		if (AWAKE.equals(consciousness)) {
			steeringComponent.update(deltaTime);
		} else if (KNOCKED_UNCONSCIOUS.equals(consciousness)) {
			return;
		}

		if (stunTime > 0) {
			stunTime -= deltaTime;
			if (stunTime < 0) {
				stunTime = 0;
			}
			return;
		}

		try {
			currentGoal.update(deltaTime, gameContext);
		} catch (SwitchGoalException e) {
			AssignedGoal newGoal = new AssignedGoal(e.target, parentEntity, messageDispatcher);
			newGoal.setAssignedJob(currentGoal.getAssignedJob());
			newGoal.setAssignedHaulingAllocation(currentGoal.getAssignedHaulingAllocation());
			newGoal.setLiquidAllocation(currentGoal.getLiquidAllocation());
			if (newGoal.getAssignedHaulingAllocation() == null && currentGoal.getAssignedJob() != null) {
				newGoal.setAssignedHaulingAllocation(currentGoal.getAssignedJob().getHaulingAllocation());
			}
			currentGoal = newGoal;
		}
	}

	public AssignedGoal getCurrentGoal() {
		return currentGoal;
	}

	protected AssignedGoal pickNextGoalFromQueue(GameContext gameContext) {
		if (parentEntity.isOnFire()) {
			return onFireGoal(gameContext);
		}

		// FIXME these are static due to re-use from CreatureBehaviour
		Optional<Memory> attackedMemory = getMemoryOfAttackedByCreature(parentEntity, null, gameContext);
		if (attackedMemory.isPresent()) {
			return attackedByCreatureResponse(attackedMemory.get(), gameContext);
		}

		MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);

		// (Override) if we're hauling an item, need to place it
		if (parentEntity.getComponent(HaulingComponent.class) != null) {
			Entity hauledEntity = parentEntity.getComponent(HaulingComponent.class).getHauledEntity();
			if (hauledEntity != null) {
				// need somewhere to place it

				HaulingAllocation stockpileAllocation = null;
				// Special case - if recently attempted to place item and failed, just dump it instead
				boolean recentlyFailedPlaceItemGoal = memoryComponent
						.getShortTermMemories(gameContext.getGameClock())
						.stream()
						.anyMatch(m -> m.getType().equals(MemoryType.FAILED_GOAL) && PLACE_ITEM.goalName.equals(m.getRelatedGoalName()));

				if (!recentlyFailedPlaceItemGoal) {
					// Temp un-requestAllocation
					ItemAllocationComponent itemAllocationComponent = hauledEntity.getComponent(ItemAllocationComponent.class);
					if (itemAllocationComponent == null) {
						itemAllocationComponent = new ItemAllocationComponent();
						itemAllocationComponent.init(hauledEntity, messageDispatcher, gameContext);
						hauledEntity.addComponent(itemAllocationComponent);
					}
					itemAllocationComponent.cancelAll(ItemAllocation.Purpose.HAULING);

					stockpileAllocation = findStockpileAllocation(gameContext.getAreaMap(), hauledEntity, roomStore, parentEntity);

					if (stockpileAllocation != null && stockpileAllocation.getItemAllocation() != null) {
						// Stockpile allocation found, swap from DUE_TO_BE_HAULED
						ItemAllocation newAllocation = itemAllocationComponent.swapAllocationPurpose(DUE_TO_BE_HAULED, ItemAllocation.Purpose.HAULING, stockpileAllocation.getItemAllocation().getAllocationAmount());
						stockpileAllocation.setItemAllocation(newAllocation);
					}

					// Always re-allocate remaining amount to hauling
					if (itemAllocationComponent.getNumUnallocated() > 0) {
						itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), parentEntity, ItemAllocation.Purpose.HAULING);
					}
				}

				if (stockpileAllocation == null) {
					// Couldn't find any stockpile, just go somewhere nearby and dump
					return new AssignedGoal(DUMP_ITEM.getInstance(), parentEntity, messageDispatcher);
				} else {
					AssignedGoal assignedGoal = new AssignedGoal(PLACE_ITEM.getInstance(), parentEntity, messageDispatcher);
					assignedGoal.setAssignedHaulingAllocation(stockpileAllocation);
					return assignedGoal;
				}
			}
		}

		Optional<Memory> breakdownMemory = memoryComponent.getShortTermMemories(gameContext.getGameClock())
				.stream().filter(m -> m.getType().equals(MemoryType.ABOUT_TO_HAVE_A_BREAKDOWN)).findFirst();
		if (breakdownMemory.isPresent()) {
			memoryComponent.removeByType(MemoryType.ABOUT_TO_HAVE_A_BREAKDOWN);
			long previousTantrums = memoryComponent.getLongTermMemories().stream().filter(m -> m.getType().equals(MemoryType.HAD_A_TANTRUM)).count();
			if (previousTantrums < MAX_TANTRUMS) {
				messageDispatcher.dispatchMessage(MessageType.SETTLER_TANTRUM, parentEntity);
				return tantrumGoal(gameContext);
			} else {
				messageDispatcher.dispatchMessage(MessageType.HUMANOID_INSANITY, parentEntity);
				return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
			}
		}

		AssignedGoal placeInventoryItemsGoal = checkToPlaceInventoryItems(gameContext);
		if (placeInventoryItemsGoal != null) {
			return placeInventoryItemsGoal;
		}

		Schedule schedule = ((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getRace().getBehaviour().getSchedule();
		List<ScheduleCategory> currentScheduleCategories = schedule == null ? List.of() : schedule.getCurrentApplicableCategories(gameContext.getGameClock());
		QueuedGoal nextGoal = goalQueue.popNextGoal(currentScheduleCategories);
		if (nextGoal == null) {
			return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
		}
		return new AssignedGoal(nextGoal.getGoal(), parentEntity, messageDispatcher);
	}

	private Optional<LiquidAllocation> liquidAllocation = Optional.empty();

	private AssignedGoal onFireGoal(GameContext gameContext) {
		liquidAllocation = Optional.empty();
		messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_ALLOCATION, new RequestLiquidAllocationMessage(
				parentEntity, AMOUNT_REQUIRED_TO_DOUSE_FIRE, false, true, this));

		if (liquidAllocation.isPresent()) {
			GridPoint2 accessLocation = liquidAllocation.get().getTargetZoneTile().getAccessLocation();
			float distanceToLiquidAllocation = parentEntity.getLocationComponent().getWorldOrParentPosition().dst(toVector(accessLocation));
			if (distanceToLiquidAllocation > MAX_DISTANCE_TO_DOUSE_FIRE) {
				messageDispatcher.dispatchMessage(MessageType.LIQUID_ALLOCATION_CANCELLED, liquidAllocation.get());
				liquidAllocation = Optional.empty();
			} else {
				AssignedGoal douseSelfGoal = new AssignedGoal(DOUSE_SELF.getInstance(), parentEntity, messageDispatcher);
				// return douse goal with allocation set
				douseSelfGoal.setLiquidAllocation(liquidAllocation.get());
				return douseSelfGoal;
			}
		}

		if (gameContext.getRandom().nextBoolean()) {
			return new AssignedGoal(ROLL_ON_FLOOR.getInstance(), parentEntity, messageDispatcher);
		}
		return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
	}

	private AssignedGoal tantrumGoal(GameContext gameContext) {
		GridPoint2 parentLocation = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition()).getTilePosition();

		Entity target = null;
		List<CompassDirection> directions = new ArrayList<>(List.of(CompassDirection.values()));
		Collections.shuffle(directions, gameContext.getRandom());

		for (CompassDirection direction : directions) {
			for (int distance = 1; distance < 6; distance++) {
				MapTile tile = gameContext.getAreaMap().getTile(parentLocation.x + (direction.getXOffset() * distance),
						parentLocation.y + (direction.getYOffset() * distance));
				if (tile != null) {
					for (Entity entity : tile.getEntities()) {
						if (entity.equals(parentEntity)) {
							continue;
						}

						if (entity.getType().equals(CREATURE)) {
							CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
							if (!attributes.getConsciousness().equals(DEAD) && !attributes.getConsciousness().equals(KNOCKED_UNCONSCIOUS)) {
								target = entity;
								break;
							}
						}
						if (entity.getType().equals(FURNITURE)) {
							FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
							if (!attributes.isDestroyed()) {
								target = entity;
								break;
							}
						}
					}
					if (target != null) {
						break;
					}
					if (!tile.isNavigable()) {
						break;
					}
				}
			}
			if (target != null) {
				break;
			}
		}
		// giving happiness buff even with no target found or else player gets multiple tantrum notifications until dead
		parentEntity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.HAD_A_TANTRUM);
		if (target == null) {
			return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
		} else {
			AssignedGoal assignedGoal = new AssignedGoal(ATTACK_AGGRESSOR.getInstance(), parentEntity, messageDispatcher);

			Memory tantrumMemory = new Memory(MemoryType.HAD_A_TANTRUM, gameContext.getGameClock());
			parentEntity.getComponent(MemoryComponent.class).addLongTerm(tantrumMemory);
			tantrumMemory.setRelatedEntityId(target.getId());


			assignedGoal.setRelevantMemory(tantrumMemory);
			return assignedGoal;
		}
	}

	protected AssignedGoal attackedByCreatureResponse(Memory attackedByCreatureMemory, GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		AggressionResponse aggressionResponse = attributes.getRace().getBehaviour().getAggressionResponse();
		if (aggressionResponse == null || aggressionResponse.equals(AggressionResponse.MIXED)) {
			if (gameContext.getRandom().nextBoolean()) {
				aggressionResponse = AggressionResponse.ATTACK;
			} else {
				aggressionResponse = AggressionResponse.FLEE;
			}
		}

		Goal goal;
		switch (aggressionResponse) {
			case ATTACK:
				goal = SpecialGoal.ATTACK_AGGRESSOR.getInstance();
				break;
			case FLEE:
				goal = SpecialGoal.FLEE_FROM_AGGRESSOR.getInstance();
				break;
			default:
				throw new NotImplementedException("No goal speified for aggression response: " + aggressionResponse.name());
		}
		AssignedGoal assignedGoal = new AssignedGoal(goal, parentEntity, messageDispatcher);
		assignedGoal.setRelevantMemory(attackedByCreatureMemory);
		return assignedGoal;
	}

	@Override
	public void allocationFound(Optional<LiquidAllocation> liquidAllocation) {
		this.liquidAllocation = liquidAllocation;
	}

	private AssignedGoal checkToPlaceInventoryItems(GameContext gameContext) {
		// Place an unused item into a stockpile if a space is available
		InventoryComponent inventory = parentEntity.getComponent(InventoryComponent.class);
		WeaponSelectionComponent weaponSelectionComponent = parentEntity.getOrCreateComponent(WeaponSelectionComponent.class);

		double currentGameTime = gameContext.getGameClock().getCurrentGameTime();
		for (InventoryComponent.InventoryEntry entry : inventory.getInventoryEntries()) {
			if (entry.entity.getType().equals(ITEM)) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) entry.entity.getPhysicalEntityComponent().getAttributes();

				if (weaponSelectionComponent.getSelectedWeapon().isPresent()) {
					ItemType weaponSelection = weaponSelectionComponent.getSelectedWeapon().get();
					if (attributes.getItemType().equals(weaponSelection)) {
						continue; // This is a weapon we have selected so do not drop
					}

					if (weaponSelection.getWeaponInfo().getRequiresAmmoType() != null && weaponSelection.getWeaponInfo().getRequiresAmmoType().equals(attributes.getItemType().getIsAmmoType())) {
						continue; // This is ammo for selected weapon
					}
				}

				if (entry.getLastUpdateGameTime() + attributes.getItemType().getHoursInInventoryUntilUnused() < currentGameTime) {
					// Temp un-requestAllocation
					ItemAllocationComponent itemAllocationComponent = entry.entity.getOrCreateComponent(ItemAllocationComponent.class);
					itemAllocationComponent.cancelAll(HELD_IN_INVENTORY);

					HaulingAllocation stockpileAllocation = findStockpileAllocation(gameContext.getAreaMap(), entry.entity, roomStore, parentEntity);

					if (stockpileAllocation == null) {
						itemAllocationComponent.createAllocation(attributes.getQuantity(), parentEntity, HELD_IN_INVENTORY);
					} else {
						ItemAllocation newAllocation = itemAllocationComponent.swapAllocationPurpose(DUE_TO_BE_HAULED, HELD_IN_INVENTORY, stockpileAllocation.getItemAllocation().getAllocationAmount());
						stockpileAllocation.setItemAllocation(newAllocation);

						if (itemAllocationComponent.getNumUnallocated() > 0) {
							itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), parentEntity, HELD_IN_INVENTORY);
						}

						return placeItemIntoStockpileGoal(entry.entity, stockpileAllocation);
					}
				}
			}
		}

		return null;
	}


	private AssignedGoal placeItemIntoStockpileGoal(Entity itemEntity, HaulingAllocation stockpileAllocation) {
		AssignedGoal assignedGoal = new AssignedGoal(PLACE_ITEM.getInstance(), parentEntity, messageDispatcher);
		assignedGoal.setAssignedHaulingAllocation(stockpileAllocation);
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getItemType().isEquippedWhileWorkingOnJob()) {
			// Switch to hauling component
			HaulingComponent haulingComponent = parentEntity.getOrCreateComponent(HaulingComponent.class);
			InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
			inventoryComponent.remove(itemEntity.getId());
			haulingComponent.setHauledEntity(itemEntity, messageDispatcher, parentEntity);
		}
		return assignedGoal;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		double gameTime = gameContext.getGameClock().getCurrentGameTime();
		double elapsed = gameTime - lastUpdateGameTime;
		lastUpdateGameTime = gameTime;

		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		needsComponent.update(elapsed, parentEntity, messageDispatcher);

		parentEntity.getComponent(StatusComponent.class).infrequentUpdate(elapsed);

		HappinessComponent happinessComponent = parentEntity.getOrCreateComponent(HappinessComponent.class);
		MapTile currentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (currentTile != null && currentTile.getRoof().getState().equals(TileRoofState.OPEN) &&
			gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().containsKey(STANDING)) {
			happinessComponent.add(gameContext.getMapEnvironment().getCurrentWeather().getHappinessModifiers().get(STANDING));
		}
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();

		thinkAboutRequiredEquipment(gameContext);
		addGoalsToQueue(gameContext);

		lookAtNearbyThings(gameContext);

		if (attributes.getSanity().equals(Sanity.SANE) && attributes.getConsciousness().equals(AWAKE) &&
				happinessComponent.getNetModifier() <= MIN_HAPPINESS_VALUE) {
			MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);
			memoryComponent.addShortTerm(new Memory(MemoryType.ABOUT_TO_HAVE_A_BREAKDOWN, gameContext.getGameClock()), gameContext.getGameClock());

			if (currentGoal != null) {
				if (currentGoal.goal.interruptedByLowNeeds || currentGoal.goal.interruptedByCombat) {
					currentGoal.setInterrupted(true);
				}
			}
		}
	}

	public void setCurrentGoal(AssignedGoal assignedGoal) {
		this.currentGoal = assignedGoal;
	}

	public GoalQueue getGoalQueue() {
		return goalQueue;
	}

	private void thinkAboutRequiredEquipment(GameContext gameContext) {
		WeaponSelectionComponent weaponSelectionComponent = parentEntity.getOrCreateComponent(WeaponSelectionComponent.class);

		if (weaponSelectionComponent.getSelectedWeapon().isPresent()) {
			ItemType weaponItemType = weaponSelectionComponent.getSelectedWeapon().get();

			InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
			InventoryComponent.InventoryEntry weaponInInventory = inventoryComponent.findByItemType(weaponItemType, gameContext.getGameClock());

			if (weaponInInventory == null) {
				Memory itemRequiredMemory = new Memory(MemoryType.LACKING_REQUIRED_ITEM, gameContext.getGameClock());
				itemRequiredMemory.setRelatedItemType(weaponItemType);
				// Should set required material at some point
				parentEntity.getOrCreateComponent(MemoryComponent.class).addShortTerm(itemRequiredMemory, gameContext.getGameClock());
			} else if (weaponItemType.getWeaponInfo() != null && weaponItemType.getWeaponInfo().getRequiresAmmoType() != null) {
				// check for ammo
				AmmoType requiredAmmoType = weaponItemType.getWeaponInfo().getRequiresAmmoType();

				boolean hasAmmo = inventoryComponent.getInventoryEntries().stream()
						.anyMatch(entry -> entry.entity.getType().equals(ITEM) &&
								requiredAmmoType.equals(((ItemEntityAttributes) entry.entity.getPhysicalEntityComponent().getAttributes()).getItemType().getIsAmmoType()));

				if (!hasAmmo) {
					Memory itemRequiredMemory = new Memory(MemoryType.LACKING_REQUIRED_ITEM, gameContext.getGameClock());
					itemRequiredMemory.setRelatedAmmoType(requiredAmmoType);
					// Should set required material at some point
					parentEntity.getOrCreateComponent(MemoryComponent.class).addShortTerm(itemRequiredMemory, gameContext.getGameClock());
				}
			}
		}
	}

	protected void addGoalsToQueue(GameContext gameContext) {
		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		MemoryComponent memoryComponent = parentEntity.getComponent(MemoryComponent.class);
		goalQueue.removeExpiredGoals(gameContext.getGameClock());
		for (Goal potentialGoal : goalDictionary.getAllGoals()) {
			if (potentialGoal.getSelectors().isEmpty()) {
				continue; // Don't add goals with no selectors
			}
			if (currentGoal != null && potentialGoal.equals(currentGoal.goal)) {
				continue; // Don't queue up the current goal
			}
			for (GoalSelector selector : potentialGoal.getSelectors()) {
				boolean allConditionsApply = true;
				for (GoalSelectionCondition condition : selector.conditions) {
					if (!condition.apply(gameContext.getGameClock(), needsComponent, memoryComponent)) {
						allConditionsApply = false;
						break;
					}
				}
				if (allConditionsApply) {
					goalQueue.add(new QueuedGoal(potentialGoal, selector.scheduleCategory, selector.priority, gameContext.getGameClock()));
					break;
				}
			}
		}
	}

	private void lookAtNearbyThings(GameContext gameContext) {
		StatusComponent statusComponent = parentEntity.getComponent(StatusComponent.class);
		if (statusComponent.contains(Blinded.class) || statusComponent.contains(TemporaryBlinded.class)) {
			return;
		}

		CreatureEntityAttributes parentAttributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (!parentAttributes.getConsciousness().equals(AWAKE)) {
			return;
		}

		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);

		GridPoint2 parentPosition = toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition());
		for (CompassDirection compassDirection : CompassDirection.values()) {
			for (int distance = 1; distance <= DISTANCE_TO_LOOK_AROUND; distance++) {
				GridPoint2 targetPosition = parentPosition.cpy().add(compassDirection.getXOffset() * distance, compassDirection.getYOffset() * distance);
				MapTile targetTile = gameContext.getAreaMap().getTile(targetPosition);
				if (targetTile == null || targetTile.hasWall()) {
					// Stop looking in this direction
					break;
				}

				for (Entity entityInTile : targetTile.getEntities()) {
					if (entityInTile.getType().equals(CREATURE)) {
						CreatureEntityAttributes creatureEntityAttributes = (CreatureEntityAttributes) entityInTile.getPhysicalEntityComponent().getAttributes();
						if (creatureEntityAttributes.getConsciousness().equals(DEAD)
								&& creatureEntityAttributes.getRace().equals(((CreatureEntityAttributes)parentEntity.getPhysicalEntityComponent().getAttributes()).getRace())) {
							// Saw a dead body!
							happinessComponent.add(SAW_DEAD_BODY);

							return; // TODO remove this, but for now this is the only thing to see so might as well stop looking
						}
					}
				}

			}
		}

	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return steeringComponent;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return true;
	}

	public void applyStun(Random random) {
		this.stunTime = 1f + (random.nextFloat() * 3f);
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(KNOCKED_UNCONSCIOUS)) {
			return List.of(i18nTranslator.getTranslatedString("ACTION.KNOCKED_UNCONSCIOUS"));
		}

		List<I18nText> descriptionStrings = new ArrayList<>();
		descriptionStrings.add(i18nTranslator.getCurrentGoalDescription(parentEntity, currentGoal, gameContext));
		if (stunTime > 0) {
			descriptionStrings.add(i18nTranslator.getTranslatedString("ACTION.STUNNED"));
		}
		return descriptionStrings;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Not yet implemented " + this.getClass().getSimpleName() + ".clone()");
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (currentGoal != null) {
			JSONObject currentGoalJson = new JSONObject(true);
			currentGoal.writeTo(currentGoalJson, savedGameStateHolder);
			asJson.put("currentGoal", currentGoalJson);
		}

		if (!goalQueue.isEmpty()) {
			JSONObject goalQueueJson = new JSONObject(true);
			goalQueue.writeTo(goalQueueJson, savedGameStateHolder);
			asJson.put("goalQueue", goalQueueJson);
		}

		if (steeringComponent != null) {
			JSONObject steeringComponentJson = new JSONObject(true);
			steeringComponent.writeTo(steeringComponentJson, savedGameStateHolder);
			asJson.put("steeringComponent", steeringComponentJson);
		}

		if (stunTime > 0) {
			asJson.put("stunTime", stunTime);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.goalDictionary = relatedStores.goalDictionary;
		this.roomStore = relatedStores.roomStore;

		JSONObject currentGoalJson = asJson.getJSONObject("currentGoal");
		if (currentGoalJson != null) {
			currentGoal = new AssignedGoal();
			currentGoal.readFrom(currentGoalJson, savedGameStateHolder, relatedStores);
		}

		JSONObject goalQueueJson = asJson.getJSONObject("goalQueue");
		if (goalQueueJson != null) {
			goalQueue.readFrom(goalQueueJson, savedGameStateHolder, relatedStores);
		}

		JSONObject steeringComponentJson = asJson.getJSONObject("steeringComponent");
		if (steeringComponentJson != null) {
			this.steeringComponent.readFrom(steeringComponentJson, savedGameStateHolder, relatedStores);
		}

		this.stunTime = asJson.getFloatValue("stunTime");
	}
}
