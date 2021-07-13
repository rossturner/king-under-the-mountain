package technology.rocketjump.undermount.rendering.camera;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CameraMovedMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.ScreenWriter;

import java.io.File;
import java.io.IOException;


@Singleton
public class PrimaryCameraWrapper implements GameContextAware, Persistable, Telegraph {
	private static final float XY_MOVEMENT_SPEED = 4.0f;

	public static final float ZOOM_SPEED = 10.0f;
	private final OrthographicCamera camera;
	private final ScreenWriter screenWriter;

	private Vector3 xyzVelocity = new Vector3();
	private float targetZoom;
	private Vector2 targetZoomToPosition = null;

	private int worldWidth;
	private int worldHeight;

	private final MessageDispatcher messageDispatcher;
	private float panSpeedMultiplier = 1f;

	private final float minTilesForZoom;
	private final float maxTilesForZoom;
	private float minZoom;
	private float maxZoom;

	// Screen shake variables
	private static final float TOTAL_SCREEN_SHAKE_SECONDS = 0.5f;
	private static final float SCREEN_SHAKE_SPEED = 45f;
	private static final float MAX_SCREEN_SHAKE_OFFSET = 1.2f;
	private Vector2 currentScreenShake = null;
	private boolean screenShakingToRight = true;
	private float screenShakeProgress;

	@Inject
	public PrimaryCameraWrapper(ScreenWriter screenWriter, MessageDispatcher messageDispatcher) throws IOException {
		this.screenWriter = screenWriter;
		this.messageDispatcher = messageDispatcher;
		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth() / 100.0f, Gdx.graphics.getHeight() / 100.0f);
		camera.zoom = 2.0f;
		targetZoom = 2.0f;

		JSONObject uiSettings = JSON.parseObject(FileUtils.readFileToString(new File("assets/ui/uiSettings.json")));
		minTilesForZoom = uiSettings.getFloatValue("minTilesZoom");
		maxTilesForZoom = uiSettings.getFloatValue("maxTilesZoom");
		recalculateZoomLimits();

		messageDispatcher.addListener(this, MessageType.MOVE_CAMERA_TO);
		messageDispatcher.addListener(this, MessageType.TRIGGER_SCREEN_SHAKE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MOVE_CAMERA_TO: {
				Vector2 worldPosition = (Vector2) msg.extraInfo;
				camera.position.x = worldPosition.x;
				camera.position.y = worldPosition.y;
				camera.update();
				messageDispatcher.dispatchMessage(MessageType.CAMERA_MOVED, new CameraMovedMessage(
						camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom, camera.position, getCursorWorldPosition(),
						minTilesForZoom, maxTilesForZoom));
				return true;
			}
			case MessageType.TRIGGER_SCREEN_SHAKE: {
				currentScreenShake = new Vector2();
				screenShakingToRight = true;
				screenShakeProgress = 0f;
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public void zoom(int zoomAmount) {
		targetZoom = camera.zoom + (camera.zoom * zoomAmount * 0.5f);
		if (targetZoom < minZoom) {
			targetZoom = minZoom;
		} else if (targetZoom > maxZoom) {
			targetZoom = maxZoom;
		}

		// recalculate targetZoomToPosition to maintain ratio of space around cursor
		Vector2 mousePositionRatio = new Vector2(
				(float) (Gdx.input.getX()) / (float) (Gdx.graphics.getWidth()),
				(float) (Gdx.input.getY()) / (float) (Gdx.graphics.getHeight())
		);
		Vector2 screenTileSize = new Vector2(
				camera.zoom * camera.viewportWidth,
				camera.zoom * camera.viewportHeight
		);
		Vector2 screenTileSizeAtTargetZoom = new Vector2(
				targetZoom * camera.viewportWidth,
				targetZoom * camera.viewportHeight
		);
		Vector2 tilesToLose = screenTileSize.cpy().sub(screenTileSizeAtTargetZoom);
		float tilesToLoseFromLeft = tilesToLose.x * mousePositionRatio.x;
		float tilesToLoseFromTop = tilesToLose.y * mousePositionRatio.y;
		targetZoomToPosition = new Vector2(
				(camera.position.x - (screenTileSize.x / 2f)) + tilesToLoseFromLeft + (screenTileSizeAtTargetZoom.x / 2f),
				(camera.position.y + (screenTileSize.y / 2f)) - tilesToLoseFromTop - (screenTileSizeAtTargetZoom.y / 2f)
		);
	}

	public void update(float deltaSeconds) {
		camera.position.x += (xyzVelocity.x * deltaSeconds * XY_MOVEMENT_SPEED * camera.zoom);
		if (camera.position.x < 0) {
			camera.position.x = 0;
		} else if (camera.position.x > worldWidth) {
			camera.position.x = worldWidth;
		}

		camera.position.y += (xyzVelocity.y * deltaSeconds * XY_MOVEMENT_SPEED * camera.zoom);
		if (camera.position.y < 0) {
			camera.position.y = 0;
		} else if (camera.position.y > worldHeight) {
			camera.position.y = worldHeight;
		}

		if (targetZoomToPosition != null) {
			if (xyzVelocity.x != 0 || xyzVelocity.y != 0) {
				// kill targetZoomToPosition if there is any player input to camera movement
				targetZoomToPosition = null;
			} else {
				camera.position.x += (targetZoomToPosition.x - camera.position.x) * deltaSeconds * ZOOM_SPEED;
				camera.position.y += (targetZoomToPosition.y - camera.position.y) * deltaSeconds * ZOOM_SPEED;
				if (Math.abs(targetZoomToPosition.x - camera.position.x) < 0.05f && Math.abs(targetZoomToPosition.y - camera.position.y) < 0.05f) {
					targetZoomToPosition = null;
				}
			}
		}

		if (xyzVelocity.z != 0) {
			// Zooming by key
			camera.zoom += (xyzVelocity.z * deltaSeconds * XY_MOVEMENT_SPEED * camera.zoom);
			camera.zoom = Math.min(camera.zoom, maxZoom);
			camera.zoom = Math.max(camera.zoom, minZoom);
			targetZoom = camera.zoom;
		} else {
			// Zooming by mouse wheel
			if (camera.zoom < targetZoom) {
				float difference = targetZoom - camera.zoom;
				if (difference < 0.02f) {
					camera.zoom = targetZoom;
				} else {
					camera.zoom += ZOOM_SPEED * deltaSeconds * difference;
				}
			}
			if (camera.zoom > targetZoom) {
				float difference = camera.zoom - targetZoom;
				if (difference < 0.05f) {
					camera.zoom = targetZoom;
				} else {
					camera.zoom -= ZOOM_SPEED * deltaSeconds * difference;
				}
			}

		}

		if (currentScreenShake != null) {
			updateScreenShake(deltaSeconds);
		}

		camera.update();

		if (!xyzVelocity.isZero() || camera.zoom != targetZoom) {
			messageDispatcher.dispatchMessage(MessageType.CAMERA_MOVED, new CameraMovedMessage(
					camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom, camera.position, getCursorWorldPosition(),
					minTilesForZoom, maxTilesForZoom));
		}

		screenWriter.printLine("Zoom: " + camera.zoom);
		screenWriter.printLine("Camera position: " + camera.position.x + ", " + camera.position.y);
		screenWriter.printLine("Camera viewport: " + camera.viewportWidth + ", " + camera.viewportHeight);
		screenWriter.printLine("Mouse: " + Gdx.input.getX() + ", " + Gdx.input.getY());
		Vector3 unprojected = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
		screenWriter.printLine("Unprojected: " + unprojected.x + ", " + unprojected.y);


		// recalculate targetZoomToPosition to maintain ratio of space around cursor
		screenWriter.printLine("targetZoomPosition: " + (targetZoomToPosition == null ? "null" : (targetZoomToPosition.x + ", " + targetZoomToPosition.y)));
	}

	private void updateScreenShake(float deltaSeconds) {
		// Undo current effects of shake
		camera.position.x -= currentScreenShake.x;
		camera.position.y -= currentScreenShake.y;

		float shakeAmount = deltaSeconds * SCREEN_SHAKE_SPEED;
		screenShakeProgress += deltaSeconds;

		if (screenShakingToRight) {
			currentScreenShake.x += shakeAmount;
			if (currentScreenShake.x > MAX_SCREEN_SHAKE_OFFSET) {
				currentScreenShake.x = MAX_SCREEN_SHAKE_OFFSET;
				screenShakingToRight = false;
			}
		} else {
			currentScreenShake.x -= shakeAmount;
			if (currentScreenShake.x < -MAX_SCREEN_SHAKE_OFFSET) {
				currentScreenShake.x = -MAX_SCREEN_SHAKE_OFFSET;
				screenShakingToRight = true;
			}
		}

		// Re-apply current shake
		camera.position.x += currentScreenShake.x;
		camera.position.y += currentScreenShake.y;

		if (screenShakeProgress > TOTAL_SCREEN_SHAKE_SECONDS) {
			camera.position.x -= currentScreenShake.x;
			camera.position.y -= currentScreenShake.y;
			this.currentScreenShake = null;
		}
	}

	public void init(TiledMap areaMap) {
		camera.zoom = 2.0f;
		targetZoom = 2.0f;
		this.worldWidth = areaMap.getWidth();
		this.worldHeight = areaMap.getHeight();
		camera.position.x = worldWidth / 2;
		camera.position.y = worldHeight / 2;

		if (areaMap.getEmbarkPoint() != null) {
			moveTo(areaMap.getEmbarkPoint());
		}

		messageDispatcher.dispatchMessage(MessageType.CAMERA_MOVED, new CameraMovedMessage(
				camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom, camera.position, getCursorWorldPosition(),
				minTilesForZoom, maxTilesForZoom));
	}

	public OrthographicCamera getCamera() {
		return camera;
	}

	public void setMovementX(float amount) {
		xyzVelocity.x = amount * panSpeedMultiplier;
	}

	public void setMovementY(float amount) {
		xyzVelocity.y = amount * panSpeedMultiplier;
	}

	public void setMovementZ(float amount) {
		xyzVelocity.z = amount;
	}

	public void onResize(int width, int height) {
		float tempX = camera.position.x;
		float tempY = camera.position.y;
		float zoom = camera.zoom;
		camera.setToOrtho(false, width / 100.0f, height / 100.0f);
		camera.position.x = tempX;
		camera.position.y = tempY;
		camera.zoom = zoom;

		recalculateZoomLimits();
	}

	private void recalculateZoomLimits() {
		if (GlobalSettings.DEV_MODE) {
			this.minZoom = 0.3f;
			this.maxZoom = 20f;
		} else {
			this.minZoom = minTilesForZoom / camera.viewportWidth;
			this.maxZoom = maxTilesForZoom / camera.viewportWidth;
		}
	}

	public void moveTo(GridPoint2 tilePosition) {
		camera.position.x = tilePosition.x + 0.5f;
		camera.position.y = tilePosition.y + 0.5f;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		if (gameContext != null && gameContext.getAreaMap() != null) {
			init(gameContext.getAreaMap());
		}
	}

	@Override
	public void clearContextRelatedState() {

	}

	public void setPanSpeedMultiplier(boolean setMultiplier) {
		if (setMultiplier) {
			panSpeedMultiplier = 4f;
			xyzVelocity.x = xyzVelocity.x * panSpeedMultiplier;
			xyzVelocity.y = xyzVelocity.y * panSpeedMultiplier;
		} else {
			panSpeedMultiplier = 1f;
			xyzVelocity.x = xyzVelocity.x / 4f;
			xyzVelocity.y = xyzVelocity.y / 4f;
		}
	}

	private Vector2 getCursorWorldPosition() {
		int screenX = Gdx.input.getX();
		int screenY = Gdx.input.getY();
		Vector3 unprojected = camera.unproject(new Vector3(screenX, screenY, 0));
		return new Vector2(unprojected.x, unprojected.y);
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		JSONObject asJson = savedGameStateHolder.cameraJson;

		asJson.put("positionX", camera.position.x);
		asJson.put("positionY", camera.position.y);
		asJson.put("zoom", camera.zoom);

		savedGameStateHolder.setCamera(this.camera);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		camera.position.x = asJson.getFloatValue("positionX");
		camera.position.y = asJson.getFloatValue("positionY");
		camera.zoom = asJson.getFloatValue("zoom");
		targetZoom = camera.zoom;
		xyzVelocity = new Vector3();
		panSpeedMultiplier = 1f;
	}
}
