package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.gamecontext.GameContext;

import static technology.rocketjump.undermount.misc.VectorUtils.toVector;

public class GoToDrinkLocationAction extends GoToLocationAction {
	public GoToDrinkLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getLiquidAllocation() == null || parent.getLiquidAllocation().getTargetZoneTile().getAccessLocation() == null) {
			return null;
		}
		return toVector(parent.getLiquidAllocation().getTargetZoneTile().getAccessLocation());
	}

}
