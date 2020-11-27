package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.messaging.MessageType;

public class ManagementScreenInputHandler implements InputProcessor {

	private final MessageDispatcher messageDispatcher;

	public ManagementScreenInputHandler(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		if (keycode == Input.Keys.ESCAPE) {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
			return true;
		}
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (button == Input.Buttons.RIGHT) {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}
