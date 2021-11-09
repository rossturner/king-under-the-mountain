package technology.rocketjump.undermount.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.ai.memory.MemoryType;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.undermount.entities.behaviour.items.ProjectileBehaviour;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.entities.model.physical.creature.body.*;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageEffect;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageLevel;
import technology.rocketjump.undermount.entities.model.physical.creature.status.*;
import technology.rocketjump.undermount.entities.model.physical.furniture.EntityDestructionCause;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static technology.rocketjump.undermount.entities.FireMessageHandler.blackenedColor;
import static technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartDamageLevel.BrokenBones;
import static technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartDamageLevel.Destroyed;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

@Singleton
public class CombatMessageHandler implements Telegraph, GameContextAware {

	private static final float NORMAL_CHANCE_TO_HIT = 0.7f;
	private static final float CHANCE_TO_HIT_WHEN_BLINDED = 0.05f;
	private static final int DAMAGE_TO_DESTROY_FURNITURE = 10;
	private final MessageDispatcher messageDispatcher;
	private final ItemEntityFactory itemEntityFactory;
	private GameContext gameContext;

	@Inject
	public CombatMessageHandler(MessageDispatcher messageDispatcher, ItemEntityFactory itemEntityFactory) {
		this.messageDispatcher = messageDispatcher;
		this.itemEntityFactory = itemEntityFactory;

		messageDispatcher.addListener(this, MessageType.MAKE_ATTACK_WITH_WEAPON);
		messageDispatcher.addListener(this, MessageType.COMBAT_PROJECTILE_REACHED_TARGET);
		messageDispatcher.addListener(this, MessageType.APPLY_ATTACK_DAMAGE);
		messageDispatcher.addListener(this, MessageType.CREATURE_DAMAGE_APPLIED);
		messageDispatcher.addListener(this, MessageType.CREATURE_ORGAN_DAMAGE_APPLIED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MAKE_ATTACK_WITH_WEAPON: {
				handleAttackWithWeapon((CombatAttackMessage) msg.extraInfo);
				return true;
			}
			case MessageType.COMBAT_PROJECTILE_REACHED_TARGET: {
				handleProjectileImpact((CombatAttackMessage) msg.extraInfo);
				return true;
			}
			case MessageType.APPLY_ATTACK_DAMAGE: {
				applyAttackDamage((CombatAttackMessage) msg.extraInfo);
				return true;
			}
			case MessageType.CREATURE_DAMAGE_APPLIED: {
				applyDamageToCreature((CreatureDamagedMessage) msg.extraInfo);
				return true;
			}
			case MessageType.CREATURE_ORGAN_DAMAGE_APPLIED: {
				applyOrganDamageToCreature((CreatureOrganDamagedMessage) msg.extraInfo);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void handleAttackWithWeapon(CombatAttackMessage attackMessage) {
		boolean isRangedAttack = attackMessage.weaponItemType.getWeaponInfo().getRange() > 1 && attackMessage.weaponItemType.getWeaponInfo().getRequiresAmmoType() != null;

		if (isRangedAttack) {
			// create ongoing effect of arrow moving towards target with rotation set
			if (attackMessage.ammoAttributes != null) {
				createProjectile(attackMessage);
				if (attackMessage.weaponItemType.getWeaponInfo().getFireWeaponSoundAsset() != null) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
						attackMessage.weaponItemType.getWeaponInfo().getFireWeaponSoundAsset(), attackMessage.attackerEntity
					));
				}
			}
		} else {
			// is a melee attack
			boolean attackHits = gameContext.getRandom().nextFloat() < getChanceToHitWithAttack(attackMessage.attackerEntity);
			if (attackHits) {
				applyAttackDamage(attackMessage);
			}
			triggerHitOrMissSound(attackMessage, attackHits);
		}
	}

	private void triggerHitOrMissSound(CombatAttackMessage attackMessage, boolean attackHits) {
		if (attackHits) {
			if (attackMessage.weaponItemType.getWeaponInfo().getWeaponHitSoundAsset() != null) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
						attackMessage.weaponItemType.getWeaponInfo().getWeaponHitSoundAsset(), attackMessage.defenderEntity
				));
			}
		} else {
			if (attackMessage.weaponItemType.getWeaponInfo().getWeaponMissSoundAsset() != null) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
						attackMessage.weaponItemType.getWeaponInfo().getWeaponMissSoundAsset(), attackMessage.defenderEntity
				));
			}
		}
	}

	private void handleProjectileImpact(CombatAttackMessage attackMessage) {
		boolean attackHits = gameContext.getRandom().nextFloat() < getChanceToHitWithAttack(attackMessage.attackerEntity);
		if (attackHits) {
			applyAttackDamage(attackMessage);
		}
		triggerHitOrMissSound(attackMessage, attackHits);
	}

	private void createProjectile(CombatAttackMessage attackMessage) {
		Vector2 attackerLocation = attackMessage.attackerEntity.getLocationComponent().getWorldOrParentPosition();
		ItemEntityAttributes ammoAttributes = attackMessage.ammoAttributes.clone();
		ammoAttributes.setItemPlacement(ItemPlacement.PROJECTILE);
		Entity projectileEntity = itemEntityFactory.create(ammoAttributes, toGridPoint(attackerLocation), false, gameContext);
		projectileEntity.getLocationComponent().setWorldPosition(attackerLocation, false);
		ProjectileBehaviour projectileBehaviour = new ProjectileBehaviour();
		projectileBehaviour.setAttackerEntity(attackMessage.attackerEntity);
		projectileBehaviour.setDefenderEntity(attackMessage.defenderEntity);
		projectileBehaviour.setWeaponItemType(attackMessage.weaponItemType);
		projectileBehaviour.init(projectileEntity, messageDispatcher, gameContext);
		projectileEntity.replaceBehaviourComponent(projectileBehaviour);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, projectileEntity);
	}

	private float getChanceToHitWithAttack(Entity attackerEntity) {
		StatusComponent statusComponent = attackerEntity.getComponent(StatusComponent.class);
		if (statusComponent.contains(Blinded.class) || statusComponent.contains(TemporaryBlinded.class)) {
			return CHANCE_TO_HIT_WHEN_BLINDED;
		} else {
			return NORMAL_CHANCE_TO_HIT;
		}
	}

	private void applyAttackDamage(CombatAttackMessage attackMessage) {
		int damageAmount = attackMessage.weaponItemType.getWeaponInfo().getMinDamage() + gameContext.getRandom().nextInt(
				attackMessage.weaponItemType.getWeaponInfo().getMaxDamage() - attackMessage.weaponItemType.getWeaponInfo().getMinDamage()
		);

		if (attackMessage.weaponItemType.getWeaponInfo().isModifiedByStrength()) {
			damageAmount += getStrengthModifier(attackMessage.attackerEntity);
		}

		CombatDamageType damageType = attackMessage.weaponItemType.getWeaponInfo().getDamageType();


		// reduce by target's damage reduction
		if (attackMessage.defenderEntity.getType().equals(EntityType.CREATURE)) {
			CreatureEntityAttributes attributes = (CreatureEntityAttributes) attackMessage.defenderEntity.getPhysicalEntityComponent().getAttributes();
			if (attributes.getRace().getFeatures().getSkin() != null) {
				damageAmount -= attributes.getRace().getFeatures().getSkin().getDamageReduction().getOrDefault(damageType, 0);
			}

			// TODO reduce by armour damage reduction

			if (attackMessage.attackerEntity.getType().equals(EntityType.CREATURE)) {
				MemoryComponent memoryComponent = attackMessage.defenderEntity.getOrCreateComponent(MemoryComponent.class);
				Memory memory = new Memory(MemoryType.ATTACKED_BY_CREATURE, gameContext.getGameClock());
				memory.setRelatedEntityId(attackMessage.attackerEntity.getId());
				memoryComponent.addShortTerm(memory, gameContext.getGameClock());
			}

			if (damageAmount <= 0) {
				return;
			}

			BodyPart impactedBodyPart = attributes.getBody().randomlySelectPartBasedOnSize(gameContext.getRandom());
			BodyPartDamage currentDamage = attributes.getBody().getDamage(impactedBodyPart);
			Optional<BodyPartOrgan> impactedOrgan = impactedBodyPart.rollToHitOrgan(gameContext.getRandom(), currentDamage);

			if (impactedOrgan.isPresent()) {
				BodyPartOrgan targetOrgan = impactedOrgan.get();
				OrganDamageLevel currentOrganDamage = attributes.getBody().getOrganDamage(impactedBodyPart, targetOrgan);
				damageAmount += currentOrganDamage.furtherDamageModifier;
				OrganDamageLevel newOrganDamage = OrganDamageLevel.getForDamageAmount(damageAmount);
				if (newOrganDamage.isGreaterThan(currentOrganDamage)) {
					attributes.getBody().setOrganDamage(impactedBodyPart, targetOrgan, newOrganDamage);
					messageDispatcher.dispatchMessage(MessageType.CREATURE_ORGAN_DAMAGE_APPLIED, new CreatureOrganDamagedMessage(
							attackMessage.defenderEntity, impactedBodyPart, targetOrgan, newOrganDamage
					));
				}
			} else {
				// impacted with body part only
				damageAmount += currentDamage.getDamageLevel().furtherDamageModifier;
				BodyPartDamageLevel newDamageLevel = BodyPartDamageLevel.getForDamageAmount(damageAmount);
				if (newDamageLevel.isGreaterThan(currentDamage.getDamageLevel())) {
					attributes.getBody().setDamage(impactedBodyPart, newDamageLevel);
					messageDispatcher.dispatchMessage(MessageType.CREATURE_DAMAGE_APPLIED, new CreatureDamagedMessage(
							attackMessage.defenderEntity, impactedBodyPart, newDamageLevel
					));

					if (newDamageLevel.equals(Destroyed)) {
						bodyPartDestroyed(impactedBodyPart, attributes.getBody(), attackMessage.defenderEntity);
					}
				}
			}

			Vector2 knockbackVector = attackMessage.defenderEntity.getLocationComponent().getWorldOrParentPosition().cpy().sub(
					attackMessage.attackerEntity.getLocationComponent().getWorldOrParentPosition()
			).nor().scl(damageAmount / 4f);
			attackMessage.defenderEntity.getBehaviourComponent().getSteeringComponent().setKnockback(knockbackVector);
		} else if (attackMessage.defenderEntity.getType().equals(EntityType.FURNITURE)) {
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) attackMessage.defenderEntity.getPhysicalEntityComponent().getAttributes();
			attributes.setDamageAmount(attributes.getDamageAmount() + damageAmount);

			if (attributes.getDamageAmount() > DAMAGE_TO_DESTROY_FURNITURE) {
				messageDispatcher.dispatchMessage(MessageType.DAMAGE_FURNITURE, new FurnitureDamagedMessage(
						attackMessage.defenderEntity, EntityDestructionCause.TANTRUM, null,
						blackenedColor(gameContext.getRandom()), blackenedColor(gameContext.getRandom())
				));
			}
		} else {
			Logger.warn("TODO: Damage application to non-creature entities");
		}
	}

	private void applyDamageToCreature(CreatureDamagedMessage message) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) message.targetCreature.getPhysicalEntityComponent().getAttributes();
		StatusComponent statusComponent = message.targetCreature.getComponent(StatusComponent.class);

		switch (message.damageLevel) {
			case None:
				return;
			case Bruised:
				if (attributes.getRace().getFeatures().getBlood() == null) {
					// This creature does not have blood, so it is not affected by bruised
					attributes.getBody().setDamage(message.impactedBodyPart, BodyPartDamageLevel.None);
					return;
				}
			case Bleeding:
				if (attributes.getRace().getFeatures().getBlood() == null) {
					// This creature does not have blood, so it is not affected by bleeding
					attributes.getBody().setDamage(message.impactedBodyPart, BodyPartDamageLevel.None);
					return;
				} else {
					statusComponent.apply(new Bleeding());
				}
				break;
			case BrokenBones:
				if (attributes.getRace().getFeatures().getBones() == null) {
					// This creature does not have bones, so it is unaffected
					attributes.getBody().setDamage(message.impactedBodyPart, BodyPartDamageLevel.None);
					return;
				}
				break;
		}

		if (gameContext.getRandom().nextFloat() < message.damageLevel.chanceToCauseStun) {
			applyStun(message.targetCreature);
		}

		if (gameContext.getRandom().nextFloat() < message.damageLevel.chanceToGoUnconscious) {
			statusComponent.apply(new KnockedUnconscious());
		}

		if (message.damageLevel.equals(BrokenBones) || message.damageLevel.equals(Destroyed)) {
			statusComponent.apply(new MovementImpaired());
		}
	}

	private void applyStun(Entity targetCreature) {
		if (targetCreature.getBehaviourComponent() instanceof SettlerBehaviour) {
			((SettlerBehaviour) targetCreature.getBehaviourComponent()).applyStun(gameContext.getRandom());
		} else if (targetCreature.getBehaviourComponent() instanceof CreatureBehaviour) {
			((CreatureBehaviour) targetCreature.getBehaviourComponent()).applyStun(gameContext.getRandom());
		}
		// else probably already dead or else inanimate
	}

	private void applyOrganDamageToCreature(CreatureOrganDamagedMessage message) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) message.targetEntity.getPhysicalEntityComponent().getAttributes();
		StatusComponent statusComponent = message.targetEntity.getComponent(StatusComponent.class);

		List<BodyPartOrgan> otherOrgansOfType = new ArrayList<>();
		for (BodyPart bodyPart : attributes.getBody().getAllBodyParts()) {
			for (BodyPartOrgan organForBodyPart : bodyPart.getPartDefinition().getOrgans()) {
				if (message.impactedOrgan.getOrganDefinition().equals(organForBodyPart.getOrganDefinition()) &&
					message.impactedOrgan.getDiscriminator() != organForBodyPart.getDiscriminator() &&
						!attributes.getBody().getOrganDamage(bodyPart, organForBodyPart).equals(OrganDamageLevel.DESTROYED)) {
					otherOrgansOfType.add(organForBodyPart);
				}
			}
		}

		boolean finalOrganInstance = otherOrgansOfType.isEmpty();

		Map<OrganDamageLevel, OrganDamageEffect> damageEffectMap = finalOrganInstance ?
				message.impactedOrgan.getOrganDefinition().getDamage().getFinalInstance() :
				message.impactedOrgan.getOrganDefinition().getDamage().getOther();

		OrganDamageEffect organDamageEffect = damageEffectMap.get(message.organDamageLevel);
		if (organDamageEffect != null) {
			switch (organDamageEffect) {
				case DEAD:
					messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH,
							new CreatureDeathMessage(message.targetEntity, DeathReason.CRITICAL_ORGAN_DAMAGE));
					break;
				case BLINDED:
					statusComponent.apply(new Blinded());
					break;
				case STUNNED:
					applyStun(message.targetEntity);
					break;
				case BLEEDING:
					statusComponent.apply(new Bleeding());
					break;
				case SUFFOCATION:
					messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH,
							new CreatureDeathMessage(message.targetEntity, DeathReason.SUFFOCATION));
					break;
				case VISION_IMPAIRED:
					statusComponent.apply(new TemporaryBlinded());
					break;
				case INTERNAL_BLEEDING:
					statusComponent.apply(new InternalBleeding());
					break;
				default:
					Logger.error("Unrecognised " + OrganDamageEffect.class.getSimpleName() + ": ");
			}
		}
	}

	private void bodyPartDestroyed(BodyPart impactedBodyPart, Body body, Entity targetEntity) {
		for (BodyPartOrgan organ : impactedBodyPart.getPartDefinition().getOrgans()) {
			if (!body.getOrganDamage(impactedBodyPart, organ).equals(OrganDamageLevel.DESTROYED)) {
				body.setOrganDamage(impactedBodyPart, organ, OrganDamageLevel.DESTROYED);
				messageDispatcher.dispatchMessage(MessageType.CREATURE_ORGAN_DAMAGE_APPLIED, new CreatureOrganDamagedMessage(
						targetEntity, impactedBodyPart, organ, OrganDamageLevel.DESTROYED
				));
			}
		}

		for (String childPartName : impactedBodyPart.getPartDefinition().getChildParts()) {
			String[] split = childPartName.split("-");
			BodyPartDiscriminator childDiscriminator = null;
			if (split.length > 1) {
				childDiscriminator = EnumUtils.getEnum(BodyPartDiscriminator.class, split[0]);
				childPartName = split[1];
			}
			BodyPartDefinition childPartDefinition = body.getBodyStructure().getPartDefinitionByName(childPartName).orElse(null);
			if (childDiscriminator == null) {
				childDiscriminator = impactedBodyPart.getDiscriminator();
			}
			final BodyPartDiscriminator finalChildDiscriminator = childDiscriminator;

			body.getAllBodyParts()
					.stream().filter(b -> b.getPartDefinition().equals(childPartDefinition) && b.getDiscriminator() == finalChildDiscriminator)
					.forEach(b -> {
						if (!body.getDamage(b).getDamageLevel().equals(Destroyed)) {
							bodyPartDestroyed(b, body, targetEntity);
						}
					});
		}

	}

	private int getStrengthModifier(Entity attackerEntity) {
		if (attackerEntity.getType().equals(EntityType.CREATURE)) {
			CreatureEntityAttributes attributes = (CreatureEntityAttributes) attackerEntity.getPhysicalEntityComponent().getAttributes();
			float strength = attributes.getStrength();
			return getAbilityScoreModifier(Math.round(strength));
		}
		return 0;
	}

	public static int getAbilityScoreModifier(int score) {
		return (score / 3) - 3;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
