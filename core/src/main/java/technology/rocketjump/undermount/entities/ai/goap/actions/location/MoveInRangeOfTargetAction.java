package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.misc.VectorUtils;

import java.util.List;

import static technology.rocketjump.undermount.entities.ai.goap.actions.AttackTargetAction.getEquippedWeaponItemType;

public class MoveInRangeOfTargetAction extends GoToLocationAction {

	public MoveInRangeOfTargetAction(AssignedGoal parent) {
		super(parent);
	}


	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		Long targetId = getTargetId();
		if (targetId == null) {
			Logger.error("Was expecting a target for " + this.getSimpleName());
			return null;
		}

		Entity targetEntity = gameContext.getEntities().get(targetId);
		if (targetEntity != null) {
			return targetEntity.getLocationComponent().getWorldOrParentPosition();
		} else {
			return null;
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

	@Override
	protected void checkForCompletion(GameContext gameContext) {
		super.checkForCompletion(gameContext);

		if (completionType == null) {
			Entity targetEntity = gameContext.getEntities().get(getTargetId());
			if (targetEntity != null) {

				ItemType equippedWeaponType = getEquippedWeaponItemType(parent.parentEntity);

				float distanceToTarget = parent.parentEntity.getLocationComponent().getWorldOrParentPosition().dst(
						targetEntity.getLocationComponent().getWorldOrParentPosition()
				);

				if (distanceToTarget <= equippedWeaponType.getWeaponInfo().getRange() && hasLineOfSightBetween(parent.parentEntity, targetEntity, gameContext)) {
					completionType = CompletionType.SUCCESS;
				}
			}
		}
	}

	public static boolean hasLineOfSightBetween(Entity parentEntity, Entity targetEntity, GameContext gameContext) {
		List<GridPoint2> locationsToCheck = VectorUtils.getGridpointsBetween(
				parentEntity.getLocationComponent().getWorldOrParentPosition(),
				targetEntity.getLocationComponent().getWorldOrParentPosition()
		);
		return locationsToCheck.stream().noneMatch(p -> {
			MapTile tile = gameContext.getAreaMap().getTile(p);
			return tile == null || tile.hasWall();
		});
	}

}
