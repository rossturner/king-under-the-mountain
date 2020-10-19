package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.Vector2;

public class DebugMessage {

	private final Vector2 worldPosition;

	public DebugMessage(Vector2 worldPosition) {
		this.worldPosition = worldPosition;
	}

	public Vector2 getWorldPosition() {
		return worldPosition;
	}
}
