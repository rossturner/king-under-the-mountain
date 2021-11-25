package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.ai.goap.SpecialGoal;
import technology.rocketjump.undermount.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.ai.memory.MemoryType;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.WeaponSelectionComponent;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.AmmoType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.WeaponInfo;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CombatAttackMessage;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.entities.ai.goap.actions.IdleAction.MAX_SEPARATION_FROM_CREATURE_GROUP;
import static technology.rocketjump.undermount.entities.ai.goap.actions.location.MoveInRangeOfTargetAction.hasLineOfSightBetween;
import static technology.rocketjump.undermount.entities.ai.goap.actions.location.MoveInRangeOfTargetAction.neatestTileToFurniture;
import static technology.rocketjump.undermount.entities.model.EntityType.*;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;
import static technology.rocketjump.undermount.ui.views.EntitySelectedGuiView.hasSelectedWeaponAndAmmoInInventory;

public class AttackTargetAction extends Action {

	private static final float TIME_TO_AIM = 1.5f;
	private static final float TIME_AFTER_ATTACK = 0.5f;
	public static final float EXTRA_RANGE_FOR_ATTACK_MULTIPLIER = 1.5f;
	private AttackTargetActionState state = AttackTargetActionState.INITIAL;
	private float stateElapsedTime;

	public AttackTargetAction(AssignedGoal parent) {
		super(parent);
	}

	public enum AttackTargetActionState {

		INITIAL,
		AIMING,
		ATTACKING,
		POST_ATTACK

	}

	@Override
	public CompletionType isCompleted(GameContext gameContext) throws SwitchGoalException {
		Entity targetEntity = gameContext.getEntities().get(getTargetId());
		if (targetEntity == null) {
			completionType = CompletionType.SUCCESS;
		} else if (targetEntity.getType().equals(CREATURE)) {
			CreatureEntityAttributes attributes = (CreatureEntityAttributes) targetEntity.getPhysicalEntityComponent().getAttributes();
			if (attributes.getConsciousness().equals(Consciousness.KNOCKED_UNCONSCIOUS) ||
					attributes.getConsciousness().equals(Consciousness.DEAD)) {
				completionType = CompletionType.SUCCESS;

				// Create fake hauling allocation to pick up entity
				HaulingAllocation haulingAllocation = new HaulingAllocation();
				haulingAllocation.setTargetPosition(toGridPoint(targetEntity.getLocationComponent().getWorldOrParentPosition()));
				haulingAllocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.FLOOR);
				haulingAllocation.setTargetId(targetEntity.getId());
				haulingAllocation.setHauledEntityId(targetEntity.getId());
				haulingAllocation.setHauledEntityType(CREATURE);
				parent.setAssignedHaulingAllocation(haulingAllocation);
			}
		} else if (targetEntity.getType().equals(FURNITURE)) {
			FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) targetEntity.getPhysicalEntityComponent().getAttributes();
			if (furnitureEntityAttributes.isDestroyed()) {
				completionType = CompletionType.SUCCESS;
			}
		}

		return super.isCompleted(gameContext);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		stateElapsedTime += deltaTime;

		switch (state) {
			case INITIAL:
 				initialCheck(gameContext);
				break;
			case AIMING:
				Entity targetEntity = gameContext.getEntities().get(getTargetId());
				Vector2 vectorToTarget = targetEntity.getLocationComponent().getWorldOrParentPosition().cpy()
						.sub(parent.parentEntity.getLocationComponent().getWorldPosition());
				parent.parentEntity.getLocationComponent().setFacing(vectorToTarget);
				if (stateElapsedTime > TIME_TO_AIM) {
					stateElapsedTime = 0;
					state = AttackTargetActionState.ATTACKING;
				}
				break;
			case ATTACKING:
				attack(gameContext);
				state = AttackTargetActionState.POST_ATTACK;
				break;
			case POST_ATTACK:
				if (stateElapsedTime > TIME_AFTER_ATTACK) {
					stateElapsedTime = 0;
					state = AttackTargetActionState.INITIAL;
				}
				break;
		}
	}

	private Long getTargetId() {
		if (parent.getAssignedJob() != null && parent.getAssignedJob().getTargetId() != null) {
			return parent.getAssignedJob().getTargetId();
		}

		if (parent.getRelevantMemory() != null) {
			return parent.getRelevantMemory().getRelatedEntityId();
		}

		return null;
	}

	private void attack(GameContext gameContext) {
		Entity targetEntity = gameContext.getEntities().get(getTargetId());
		if (targetEntity == null) {
			completionType = CompletionType.FAILURE;
			return;
		}

		ItemType equippedWeaponType = getEquippedWeaponItemType(parent.parentEntity);

		float distanceToTarget = distanceTo(targetEntity, gameContext);
		if (!(distanceToTarget <= 0.5f + (equippedWeaponType.getWeaponInfo().getRange() * EXTRA_RANGE_FOR_ATTACK_MULTIPLIER))) {
			completionType = CompletionType.FAILURE;
			return;
		}

		if (!hasLineOfSightBetween(parent.parentEntity, targetEntity, gameContext)) {
			completionType = CompletionType.FAILURE;
			return;
		}

		AmmoType requiredAmmoType = equippedWeaponType.getWeaponInfo().getRequiresAmmoType();
		ItemEntityAttributes ammoAttributes = null;
		if (requiredAmmoType != null) {
			ammoAttributes = decrementAmmoFromInventory(requiredAmmoType);
		}

		parent.messageDispatcher.dispatchMessage(MessageType.MAKE_ATTACK_WITH_WEAPON,
				new CombatAttackMessage(parent.parentEntity, targetEntity, equippedWeaponType, ammoAttributes));

	}

	private void initialCheck(GameContext gameContext) throws SwitchGoalException {
		Long targetId = getTargetId();
		if (targetId == null) {
			Logger.error("No target for " + getSimpleName());
			completionType = CompletionType.FAILURE;
			return;
		}

		Entity targetEntity = gameContext.getEntities().get(targetId);
		if (targetEntity == null) {
			completionType = CompletionType.FAILURE;
			return;
		}

		if (!hasLineOfSightBetween(parent.parentEntity, targetEntity, gameContext)) {
			completionType = CompletionType.FAILURE;
			return;
		}

		WeaponSelectionComponent weaponSelectionComponent = parent.parentEntity.getOrCreateComponent(WeaponSelectionComponent.class);
		if (!hasSelectedWeaponAndAmmoInInventory(parent.parentEntity, weaponSelectionComponent.getSelectedWeapon(), gameContext)) {
			new UnequipWeaponAction(parent).update(0f, gameContext);
			throw new SwitchGoalException(SpecialGoal.ABANDON_JOB);
		}

		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour) {
			CreatureBehaviour creatureBehaviour = (CreatureBehaviour) parent.parentEntity.getBehaviourComponent();
			if (creatureBehaviour.getCreatureGroup() != null) {
				GridPoint2 homeLocation = creatureBehaviour.getCreatureGroup().getHomeLocation();
				float distanceFromHomeLocation = parent.parentEntity.getLocationComponent().getWorldOrParentPosition().dst(toVector(homeLocation));
				if (distanceFromHomeLocation > (MAX_SEPARATION_FROM_CREATURE_GROUP * 2)) {
					// when more than double from home, count as successful (i.e. have chased away opponent)
					completionType = CompletionType.SUCCESS;
					return;
				}
			}
		}

		WeaponInfo equippedWeaponInfo = getEquippedWeaponItemType(parent.parentEntity).getWeaponInfo();
		float distanceToTarget = distanceTo(targetEntity, gameContext);
		if (distanceToTarget > equippedWeaponInfo.getRange()) {
			completionType = CompletionType.FAILURE;
			return;
		}

		if (equippedWeaponInfo.getRequiresAmmoType() != null) {
			if (getAmmoInInventory(equippedWeaponInfo) == null) {
				unequipWeapon(gameContext);
				return; // next update will set to FAILED then move to unarmed range
			}
		}

		if (isATantrumWhichHasBeenOngoingAWhile(gameContext)) {
			completionType = CompletionType.SUCCESS;
			return;
		}

		state = AttackTargetActionState.AIMING;
	}

	private float distanceTo(Entity targetEntity, GameContext gameContext) {
		Vector2 targetLocation = targetEntity.getLocationComponent().getWorldOrParentPosition();
		if (targetEntity.getType().equals(FURNITURE)) {
			targetLocation = neatestTileToFurniture(targetEntity, parent.parentEntity, gameContext);
			if (targetLocation == null) {
				targetLocation = targetEntity.getLocationComponent().getWorldOrParentPosition();
			}
			return targetLocation.dst(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()) + 0.4f;
		} else {
			return targetLocation.dst(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
		}
	}

	public static ItemType getEquippedWeaponItemType(Entity entity) {
		EquippedItemComponent equippedItemComponent = entity.getComponent(EquippedItemComponent.class);
		Entity equippedItem = equippedItemComponent.getEquippedItem();

		ItemType equippedWeaponType = ItemType.UNARMED_WEAPON;
		if (equippedItem != null && equippedItem.getType().equals(ITEM)) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) equippedItem.getPhysicalEntityComponent().getAttributes();
			if (attributes.getItemType().getWeaponInfo() != null) {
				equippedWeaponType = attributes.getItemType();
			}
		}

		return equippedWeaponType;
	}

	private InventoryComponent.InventoryEntry getAmmoInInventory(WeaponInfo equippedWeaponInfo) {
		AmmoType requiredAmmoType = equippedWeaponInfo.getRequiresAmmoType();
		for (InventoryComponent.InventoryEntry inventoryEntry : parent.parentEntity.getComponent(InventoryComponent.class).getInventoryEntries()) {
			if (inventoryEntry.entity.getType().equals(ITEM)) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().getIsAmmoType() != null && attributes.getItemType().getIsAmmoType().equals(requiredAmmoType)) {
					return inventoryEntry;
				}
			}
		}

		return null;
	}

	private void unequipWeapon(GameContext gameContext) {
		new UnequipWeaponAction(parent).update(0f, gameContext);
	}

	private ItemEntityAttributes decrementAmmoFromInventory(AmmoType requiredAmmoType) {
		Optional<InventoryComponent.InventoryEntry> inventoryEntry = parent.parentEntity.getComponent(InventoryComponent.class).getInventoryEntries()
				.stream()
				.filter(e -> e.entity.getType().equals(ITEM) &&
						requiredAmmoType.equals(((ItemEntityAttributes) e.entity.getPhysicalEntityComponent().getAttributes()).getItemType().getIsAmmoType()))
				.findFirst();
		if (inventoryEntry.isPresent()) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.get().entity.getPhysicalEntityComponent().getAttributes();

			ItemEntityAttributes cloned = attributes.clone();
			cloned.setQuantity(1);

			attributes.setQuantity(attributes.getQuantity() - 1);
			if (attributes.getQuantity() <= 0) {
				parent.messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, inventoryEntry.get().entity);
			}

			return cloned;
		}
		return null;
	}

	private boolean isATantrumWhichHasBeenOngoingAWhile(GameContext gameContext) {
		MemoryComponent memoryComponent = parent.parentEntity.getOrCreateComponent(MemoryComponent.class);
		if (memoryComponent.getLongTermMemories().isEmpty()) {
			return false;
		}
		List<Memory> tantrumMemories = memoryComponent.getLongTermMemories().stream().filter(m -> m.getType().equals(MemoryType.HAD_A_TANTRUM)).collect(Collectors.toList());
		Memory mostRecentTantrum = tantrumMemories.isEmpty() ? null : tantrumMemories.get(tantrumMemories.size() - 1);
		if (mostRecentTantrum == null) {
			return false;
		} else {
			double timeSinceTantrum = gameContext.getGameClock().getCurrentGameTime() - mostRecentTantrum.getGameTimeMemoryOccurred();
			return timeSinceTantrum > 1.0 && timeSinceTantrum < 1.5; // over an hour but less than 1.5 hours
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("state", state.name());
		asJson.put("stateElapsedTime", stateElapsedTime);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.state = EnumParser.getEnumValue(asJson, "state", AttackTargetActionState.class, AttackTargetActionState.INITIAL);
		this.stateElapsedTime = asJson.getFloatValue("stateElapsedTime");
	}

}
