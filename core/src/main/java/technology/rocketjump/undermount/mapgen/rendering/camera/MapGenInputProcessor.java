package technology.rocketjump.undermount.mapgen.rendering.camera;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public class MapGenInputProcessor implements InputProcessor {

	private final OnKeyUp callback;

	public MapGenInputProcessor(OnKeyUp callback) {
		this.callback = callback;
	}

	@Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
		callback.onKeyUp(keycode);
		return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (button == Input.Buttons.LEFT) {
			callback.onKeyUp(Input.Keys.R);
		}

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
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
//        primaryCameraManager.zoom(amount);
        return true;
    }

}
