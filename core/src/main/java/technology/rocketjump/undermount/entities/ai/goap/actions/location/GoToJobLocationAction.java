package technology.rocketjump.undermount.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.ai.goap.AssignedGoal;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class GoToJobLocationAction extends GoToLocationAction {
	public GoToJobLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getAssignedJob() == null) {
			return null;
		} else {
			return getJobLocation(gameContext);
		}
	}

}
