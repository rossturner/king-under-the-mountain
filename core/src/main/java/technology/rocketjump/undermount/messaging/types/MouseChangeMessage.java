package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;

public class MouseChangeMessage {

	private final int screenX;
	private final int screenY;
	private final Vector2 worldPosition;
	private final MouseButtonType buttonType;

	public MouseChangeMessage(int screenX, int screenY, Vector2 worldPosition, MouseButtonType buttonType) {
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

	public Vector2 getWorldPosition() {
		return worldPosition;
	}

	public MouseButtonType getButtonType() {
		return buttonType;
	}

	// MODDING expose this to keybindings
	public enum MouseButtonType {

		PRIMARY_BUTTON(Input.Buttons.LEFT),
		CANCEL_BUTTON(Input.Buttons.RIGHT),
		MIDDLE_BUTTON(Input.Buttons.MIDDLE);

		private final int inputButtonCode;

		MouseButtonType(int inputButtonCode) {
			this.inputButtonCode = inputButtonCode;
		}

		public int getInputButtonCode() {
			return inputButtonCode;
		}

		public static MouseButtonType byButtonCode(int inputButtonCode) {
			for (MouseButtonType mouseButtonType : values()) {
				if (mouseButtonType.inputButtonCode == inputButtonCode) {
					return mouseButtonType;
				}
			}
			return null;
		}
	}
}
