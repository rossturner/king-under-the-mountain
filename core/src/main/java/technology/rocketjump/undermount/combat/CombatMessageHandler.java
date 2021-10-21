package technology.rocketjump.undermount.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.behaviour.items.ProjectileBehaviour;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.body.*;
import technology.rocketjump.undermount.entities.model.physical.creature.body.organs.OrganDamageLevel;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CombatAttackMessage;
import technology.rocketjump.undermount.messaging.types.CreatureDamagedMessage;
import technology.rocketjump.undermount.messaging.types.CreatureOrganDamagedMessage;

import java.util.Optional;

import static technology.rocketjump.undermount.entities.model.physical.creature.body.BodyPartDamageLevel.Destroyed;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

@Singleton
public class CombatMessageHandler implements Telegraph, GameContextAware {

	private static final float FIXED_CHANCE_TO_HIT = 0.7f;
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
			}
		} else {
			// is a melee attack
			boolean attackHits = gameContext.getRandom().nextFloat() < FIXED_CHANCE_TO_HIT;
			if (attackHits) {
				applyAttackDamage(attackMessage);
			}
		}
	}

	private void handleProjectileImpact(CombatAttackMessage attackMessage) {
		boolean attackHits = gameContext.getRandom().nextFloat() < FIXED_CHANCE_TO_HIT;
		if (attackHits) {
			applyAttackDamage(attackMessage);
		}
	}

	private void createProjectile(CombatAttackMessage attackMessage) {
		Vector2 attackerLocation = attackMessage.attackerEntity.getLocationComponent().getWorldOrParentPosition();
		Entity projectileEntity = itemEntityFactory.create(attackMessage.ammoAttributes, toGridPoint(attackerLocation), false, gameContext);
		projectileEntity.getLocationComponent().setWorldPosition(attackerLocation, true);
		ProjectileBehaviour projectileBehaviour = new ProjectileBehaviour();
		projectileBehaviour.setAttackerEntity(attackMessage.attackerEntity);
		projectileBehaviour.setDefenderEntity(attackMessage.defenderEntity);
		projectileBehaviour.setWeaponItemType(attackMessage.weaponItemType);
		projectileBehaviour.init(projectileEntity, messageDispatcher, gameContext);
		projectileEntity.replaceBehaviourComponent(projectileBehaviour);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, projectileEntity);
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
				attributes.getBody().setOrganDamage(impactedBodyPart, targetOrgan, newOrganDamage);
				messageDispatcher.dispatchMessage(MessageType.CREATURE_ORGAN_DAMAGE_APPLIED, new CreatureOrganDamagedMessage(
						attackMessage.defenderEntity, impactedBodyPart, targetOrgan, newOrganDamage
				));
				Logger.debug("Applying " + newOrganDamage + " to " + targetOrgan.getType());
			} else {
				// impacted with body part only
				damageAmount += currentDamage.getDamageLevel().furtherDamageModifier;
				BodyPartDamageLevel newDamageLevel = BodyPartDamageLevel.getForDamageAmount(damageAmount);
				attributes.getBody().setDamage(impactedBodyPart, newDamageLevel);
				messageDispatcher.dispatchMessage(MessageType.CREATURE_DAMAGE_APPLIED, new CreatureDamagedMessage(
						attackMessage.defenderEntity, impactedBodyPart, newDamageLevel
				));
				Logger.debug("Applying " + newDamageLevel + " from " + damageAmount + " damage to " + impactedBodyPart);

				if (newDamageLevel.equals(Destroyed)) {
					bodyPartDestroyed(impactedBodyPart, attributes.getBody(), attackMessage.defenderEntity);
				}
			}
		} else {
			Logger.warn("TODO: Damage application to non-creature entities");
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
