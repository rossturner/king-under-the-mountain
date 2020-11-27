package technology.rocketjump.undermount.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.MouseChangeMessage;
import technology.rocketjump.undermount.rendering.RenderingOptions;
import technology.rocketjump.undermount.rendering.camera.DisplaySettings;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.rendering.camera.PrimaryCameraWrapper;

/**
 * This class is for input directly in the game world, as compared to some input that was caught by the GUI instead
 * <p>
 * MODDING - Keybindings should be driven by a moddable file, and later by an in-game keybindings menu
 */
@Singleton
public class GameWorldInputHandler implements InputProcessor, GameContextAware {

	public static final int SCROLL_BORDER = 2;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final RenderingOptions renderingOptions;
	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	@Inject
	public GameWorldInputHandler(PrimaryCameraWrapper primaryCameraWrapper, RenderingOptions renderingOptions,
								 MessageDispatcher messageDispatcher) {
		this.primaryCameraWrapper = primaryCameraWrapper;
		this.renderingOptions = renderingOptions;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public boolean keyDown(int keycode) {
		if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
			primaryCameraWrapper.setPanSpeedMultiplier(true);
		} else if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) {
			primaryCameraWrapper.setMovementX(-1);
		} else if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) {
			primaryCameraWrapper.setMovementX(1);
		} else if (keycode == Input.Keys.W || keycode == Input.Keys.UP) {
			primaryCameraWrapper.setMovementY(1);
		} else if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN) {
			primaryCameraWrapper.setMovementY(-1);
		} else if (keycode == Input.Keys.Q || keycode == Input.Keys.PAGE_DOWN) {
			primaryCameraWrapper.setMovementZ(0.075f);
		} else if (keycode == Input.Keys.E || keycode == Input.Keys.PAGE_UP) {
			primaryCameraWrapper.setMovementZ(-0.075f);
		} else if (keycode == Input.Keys.F5) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_QUICKSAVE);
		} else if (keycode == Input.Keys.F8) {
			messageDispatcher.dispatchMessage(MessageType.TRIGGER_QUICKLOAD);
		} else {
			return false;
		}
		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		if (GlobalSettings.DEV_MODE) {
			if (keycode == Input.Keys.J) {
				renderingOptions.debug().setShowJobStatus(!renderingOptions.debug().showJobStatus());
			} else if (keycode == Input.Keys.O) {
				renderingOptions.toggleFloorOverlapRenderingEnabled();
			} else if (keycode == Input.Keys.L) {
				renderingOptions.debug().setShowIndividualLightingBuffers(!renderingOptions.debug().showIndividualLightingBuffers());
			} else if (keycode == Input.Keys.Z) {
				renderingOptions.debug().setShowZones(!renderingOptions.debug().isShowZones());
			} else if (keycode == Input.Keys.T) {
				renderingOptions.debug().setShowPathfindingNodes(!renderingOptions.debug().showPathfindingNodes());
			} else if (keycode == Input.Keys.G) {
				DisplaySettings.showGui = !DisplaySettings.showGui;
			} else if (keycode == Input.Keys.NUM_5 && gameContext != null) {
				messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED5);
			} else if (keycode == Input.Keys.NUM_6 && gameContext != null) {
				messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.VERY_SLOW);
			} else if (keycode == Input.Keys.F1) {
				messageDispatcher.dispatchMessage(MessageType.START_NEW_GAME);
			}
		}


		if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
			primaryCameraWrapper.setPanSpeedMultiplier(false);
		} else if (keycode == Input.Keys.NUM_1 && gameContext != null) {
			messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.NORMAL);
		} else if (keycode == Input.Keys.SPACE && gameContext != null) {
			messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
		} else if (keycode == Input.Keys.NUM_2 && gameContext != null) {
			messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED2);
		} else if (keycode == Input.Keys.NUM_3 && gameContext != null) {
			messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED3);
		} else if (keycode == Input.Keys.NUM_4 && gameContext != null) {
			messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED4);

		} else if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) {
			primaryCameraWrapper.setMovementX(0);
		} else if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) {
			primaryCameraWrapper.setMovementX(0);
		} else if (keycode == Input.Keys.W || keycode == Input.Keys.UP) {
			primaryCameraWrapper.setMovementY(0);
		} else if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN) {
			primaryCameraWrapper.setMovementY(0);
		} else if (keycode == Input.Keys.O || keycode == Input.Keys.P || keycode == Input.Keys.K || keycode == Input.Keys.L
				|| keycode == Input.Keys.Q || keycode == Input.Keys.E || keycode == Input.Keys.PAGE_UP || keycode == Input.Keys.PAGE_DOWN) {
			primaryCameraWrapper.setMovementZ(0);

		} else if (keycode == Input.Keys.R) {
			messageDispatcher.dispatchMessage(MessageType.ROTATE_FURNITURE);
		} else if (keycode == Input.Keys.ESCAPE) {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_MENU");
		} else {
			return false;
		}
		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (renderingOptions.debug().showIndividualLightingBuffers()) {
			screenX = renderingOptions.debug().adjustScreenXForSplitView(screenX);
			screenY = renderingOptions.debug().adjustScreenYForSplitView(screenY);
		}

		Vector3 worldPosition = primaryCameraWrapper.getCamera().unproject(new Vector3(screenX, screenY, 0));
		Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
		MouseChangeMessage.MouseButtonType mouseButtonType = MouseChangeMessage.MouseButtonType.byButtonCode(button);
		if (mouseButtonType != null) {
			MouseChangeMessage mouseChangeMessage = new MouseChangeMessage(screenX, screenY, worldPosition2, mouseButtonType);
			messageDispatcher.dispatchMessage(null, MessageType.MOUSE_DOWN, mouseChangeMessage);
			return true;
		}
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (renderingOptions.debug().showIndividualLightingBuffers()) {
			screenX = renderingOptions.debug().adjustScreenXForSplitView(screenX);
			screenY = renderingOptions.debug().adjustScreenYForSplitView(screenY);
		}

		Vector3 worldPosition = primaryCameraWrapper.getCamera().unproject(new Vector3(screenX, screenY, 0));
		Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
		MouseChangeMessage.MouseButtonType mouseButtonType = MouseChangeMessage.MouseButtonType.byButtonCode(button);
		if (mouseButtonType != null) {
			MouseChangeMessage mouseChangeMessage = new MouseChangeMessage(screenX, screenY, worldPosition2, mouseButtonType);
			messageDispatcher.dispatchMessage(null, MessageType.MOUSE_UP, mouseChangeMessage);
			return true;
		}
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		Vector3 worldPosition = primaryCameraWrapper.getCamera().unproject(new Vector3(screenX, screenY, 0));
		Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
		MouseChangeMessage mouseMovedMessage = new MouseChangeMessage(screenX, screenY, worldPosition2, null);
		messageDispatcher.dispatchMessage(null, MessageType.MOUSE_MOVED, mouseMovedMessage);
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();

		if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
				Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT) ||
				Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP) ||
				Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			return false; // Don't do anything if already scrolling by key
		}

		if (GlobalSettings.USE_EDGE_SCROLLING) {
			if (screenX <= SCROLL_BORDER) {
				primaryCameraWrapper.setMovementX(-1);
			} else if (screenX >= width - SCROLL_BORDER) {
				primaryCameraWrapper.setMovementX(1);
			} else {
				primaryCameraWrapper.setMovementX(0);
			}

			if (screenY <= SCROLL_BORDER) {
				primaryCameraWrapper.setMovementY(1);
			} else if (screenY >= height - SCROLL_BORDER) {
				primaryCameraWrapper.setMovementY(-1);
			} else {
				primaryCameraWrapper.setMovementY(0);
			}
		}


		return true;
	}

	@Override
	public boolean scrolled(int amount) {
		primaryCameraWrapper.zoom(amount);
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
