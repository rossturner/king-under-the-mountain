package technology.rocketjump.undermount.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.utils.Pools;

public class StageAreaOnlyInputHandler implements InputProcessor {

	private final Stage parent;
	private final GameInteractionStateContainer interactionStateContainer;

	public StageAreaOnlyInputHandler(Stage parent, GameInteractionStateContainer interactionStateContainer) {
		this.parent = parent;
		this.interactionStateContainer = interactionStateContainer;
	}

	@Override
	public boolean keyDown(int keycode) {
		return parent.keyDown(keycode);
	}

	@Override
	public boolean keyUp(int keycode) {
		return parent.keyUp(keycode);
	}

	@Override
	public boolean keyTyped(char character) {
		return parent.keyTyped(character);
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		Vector2 mouseStageCoords = parent.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
		Actor target = parent.hit(mouseStageCoords.x, mouseStageCoords.y, true);
		parent.touchDown(screenX, screenY, pointer, button);
		if (target == null || button == Input.Buttons.RIGHT || interactionStateContainer.isDragging()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		Vector2 mouseStageCoords = parent.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
		Actor target = parent.hit(mouseStageCoords.x, mouseStageCoords.y, true);
		parent.touchUp(screenX, screenY, pointer, button);
		if (target == null || button == Input.Buttons.RIGHT || interactionStateContainer.isDragging()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return parent.touchDragged(screenX, screenY, pointer);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		Vector2 mouseStageCoords = parent.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
		Actor target = parent.hit(mouseStageCoords.x, mouseStageCoords.y, true);

		parent.mouseMoved(screenX, screenY);
		if (target == null || interactionStateContainer.isDragging()) {
			return false;
		} else {
			return true;
		}

	}

	/**
	 * This is overriding default stage behaviour on scrolling to act where the mouse is rather than what has focus,
	 * mostly so scrolling in the game world zooms in and out while a Widget has focus
	 */
	@Override
	public boolean scrolled(int amount) {
		Vector2 mouseStageCoords = parent.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

		Actor target = parent.hit(mouseStageCoords.x, mouseStageCoords.y, true);
		target = findParentScrollable(target);
		if (target == null) {
			target = parent.getRoot();
		}

		if (target.getX() <= mouseStageCoords.x && mouseStageCoords.x <= target.getX() + target.getWidth() &&
				target.getY() <= mouseStageCoords.y && mouseStageCoords.y <= target.getY() + target.getHeight()) {
			InputEvent event = Pools.obtain(InputEvent.class);
			event.setStage(parent);
			event.setType(InputEvent.Type.scrolled);
			event.setScrollAmount(amount);
			event.setStageX(mouseStageCoords.x);
			event.setStageY(mouseStageCoords.y);
			target.fire(event);
			boolean handled = event.isHandled();
			Pools.free(event);
			return handled;
		} else {
			return false;
		}
	}

	private Actor findParentScrollable(Actor actor) {
		if (actor == null) {
			return null;
		} else if (actor instanceof ScrollPane) {
			return actor;
		} else {
			return findParentScrollable(actor.getParent());
		}
	}
}
