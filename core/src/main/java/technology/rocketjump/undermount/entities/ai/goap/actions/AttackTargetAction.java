package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.EquippedItemComponent;
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

import java.util.Optional;

import static technology.rocketjump.undermount.entities.ai.goap.actions.location.MoveInRangeOfTargetAction.hasLineOfSightBetween;
import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;

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
	public void update(float deltaTime, GameContext gameContext) {
		stateElapsedTime += deltaTime;

		switch (state) {
			case INITIAL:
				initialCheck(gameContext);
				break;
			case AIMING:
				Entity targetEntity = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
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

	private void attack(GameContext gameContext) {
		Entity targetEntity = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		if (targetEntity == null) {
			completionType = CompletionType.FAILURE;
			return;
		}

		ItemType equippedWeaponType = getEquippedWeaponItemType(parent.parentEntity);

		if (!inRange(targetEntity, 0.5f + (equippedWeaponType.getWeaponInfo().getRange() * EXTRA_RANGE_FOR_ATTACK_MULTIPLIER))) {
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

	private void initialCheck(GameContext gameContext) {
		if (parent.getAssignedJob() == null || parent.getAssignedJob().getTargetId() == null) {
			Logger.error("No target for " + getSimpleName());
			completionType = CompletionType.FAILURE;
			return;
		}

		Entity targetEntity = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		if (targetEntity == null) {
			completionType = CompletionType.FAILURE;
			return;
		}

		if (!hasLineOfSightBetween(parent.parentEntity, targetEntity, gameContext)) {
			completionType = CompletionType.FAILURE;
			return;
		}

		WeaponInfo equippedWeaponInfo = getEquippedWeaponItemType(parent.parentEntity).getWeaponInfo();

		if (!inRange(targetEntity, equippedWeaponInfo.getRange())) {
			completionType = CompletionType.FAILURE;
			return;
		}

		if (equippedWeaponInfo.getRequiresAmmoType() != null) {
			if (getAmmoInInventory(equippedWeaponInfo) == null) {
				unequipWeapon(gameContext);
				return; // next update will set to FAILED then move to unarmed range
			}
		}

		state = AttackTargetActionState.AIMING;
	}

	private boolean inRange(Entity targetEntity, float range) {
		float distanceToTarget = parent.parentEntity.getLocationComponent().getWorldOrParentPosition().dst(targetEntity.getLocationComponent().getWorldOrParentPosition());
		return distanceToTarget <= range;
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
