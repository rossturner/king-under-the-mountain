package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;

import java.util.List;

import static technology.rocketjump.undermount.entities.ai.goap.actions.AttackTargetAction.getEquippedWeaponItemType;
import static technology.rocketjump.undermount.misc.VectorUtils.getGridpointsBetween;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

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
			if (targetEntity.getType().equals(EntityType.FURNITURE)) {
				return neatestTileToFurniture(targetEntity, parent.parentEntity, gameContext);
			} else {
				return targetEntity.getLocationComponent().getWorldOrParentPosition();
			}
		} else {
			return null;
		}
	}

	public static Vector2 neatestTileToFurniture(Entity targetFurniture, Entity parentEntity, GameContext gameContext) {
		List<GridPoint2> gridpointsBetween = getGridpointsBetween(targetFurniture.getLocationComponent().getWorldPosition(), parentEntity.getLocationComponent().getWorldOrParentPosition());
		for (GridPoint2 gridPoint : gridpointsBetween) {
			MapTile tile = gameContext.getAreaMap().getTile(gridPoint);

			if (tile.getEntities().stream().anyMatch(e -> e.equals(targetFurniture))) {
				continue;
			}

			// Else this should be the nearest tile to the furniture
			if (!tile.isNavigable(parentEntity)) {
				return null;
			}

			return toVector(gridPoint);
		}
		return null;
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
		List<GridPoint2> locationsToCheck = getGridpointsBetween(
				parentEntity.getLocationComponent().getWorldOrParentPosition(),
				targetEntity.getLocationComponent().getWorldOrParentPosition()
		);
		return locationsToCheck.stream().noneMatch(p -> {
			MapTile tile = gameContext.getAreaMap().getTile(p);
			return tile == null || tile.hasWall();
		});
	}

}
