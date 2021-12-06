package technology.rocketjump.undermount.entities.planning;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Vector2;

public interface PathfindingCallback {

	void pathfindingStarted(PathfindingTask task); // only really for debugging

	void pathfindingComplete(GraphPath<Vector2> path, long relatedId);

}
