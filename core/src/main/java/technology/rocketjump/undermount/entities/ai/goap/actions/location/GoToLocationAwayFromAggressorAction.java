package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;

import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

public class GoToLocationAwayFromAggressorAction extends GoToLocationAction {

	private static final float MIN_DISTANCE_TO_FLEE = 9f;
	private static final float MAX_DISTANCE_TO_FLEE = 20f;

	public GoToLocationAwayFromAggressorAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getRelevantMemory() == null) {
			return null;
		}
		Entity aggressorEntity = gameContext.getEntities().get(parent.getRelevantMemory().getRelatedEntityId());
		if (aggressorEntity == null) {
			return null;
		}
		Vector2 parentLocation = parent.parentEntity.getLocationComponent().getWorldOrParentPosition();
		MapTile parentTile = gameContext.getAreaMap().getTile(parentLocation);
		if (parentTile == null) {
			return null;
		}
		int parentRegionId = parentTile.getRegionId();

		Vector2 aggressorLocation = aggressorEntity.getLocationComponent().getWorldOrParentPosition().cpy();
		Vector2 parentToAggressor = aggressorLocation.sub(parentLocation);
		Vector2 awayFromAggressor = new Vector2(
				0 - parentToAggressor.x,
				0 - parentToAggressor.y
		).nor();
		float distanceToFlee = (gameContext.getRandom().nextFloat() * (MAX_DISTANCE_TO_FLEE - MIN_DISTANCE_TO_FLEE)) + MIN_DISTANCE_TO_FLEE;
		awayFromAggressor.scl(distanceToFlee);

		Vector2 targetArea = parentLocation.cpy().add(awayFromAggressor);
		GridPoint2 targetTile = toGridPoint(targetArea);
		MapTile selectedTargetTile = null;

		// try to find a spot in a 5x5 area
		for (int attempt = 0; attempt < 10; attempt++) {
			MapTile possibleTile = gameContext.getAreaMap().getTile(
					targetTile.x - 2 + gameContext.getRandom().nextInt(5),
					targetTile.y - 2 + gameContext.getRandom().nextInt(5)
			);
			if (possibleTile != null && possibleTile.getRegionId() == parentRegionId && possibleTile.isNavigable(parent.parentEntity)) {
				selectedTargetTile = possibleTile;
				break;
			}
		}

		if (selectedTargetTile == null) {
			return null;
		}

		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour) {
			CreatureBehaviour creatureBehaviour = (CreatureBehaviour) parent.parentEntity.getBehaviourComponent();
			if (creatureBehaviour.getCreatureGroup() != null) {
				creatureBehaviour.getCreatureGroup().setHomeLocation(selectedTargetTile.getTilePosition());
			}
		}
		return toVector(selectedTargetTile.getTilePosition());
	}

}