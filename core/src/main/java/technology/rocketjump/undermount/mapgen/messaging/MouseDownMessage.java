package technology.rocketjump.undermount.mapgen.messaging;

import com.badlogic.gdx.math.Vector3;

public class MouseDownMessage {

	private final int screenX;
	private final int screenY;
	private final Vector3 worldPosition;
	private final MouseButtonType buttonType;

	public MouseDownMessage(int screenX, int screenY, Vector3 worldPosition, MouseButtonType buttonType) {
		this.screenX = screenX;
		this.screenY = screenY;
		this.worldPosition = worldPosition;
		this.buttonType = buttonType;
	}

	public int getScreenX() {
		return screenX;
	}

	public int getScreenY() {
		return screenY;
	}

	public Vector3 getWorldPosition() {
		return worldPosition;
	}

	public MouseButtonType getButtonType() {
		return buttonType;
	}

	public static enum MouseButtonType {
		YES, NO;
	}
}
