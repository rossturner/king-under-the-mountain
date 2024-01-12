package technology.rocketjump.undermount.mapgen.rendering.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.mapgen.model.AbstractGameMap;

public class PrimaryCameraManager {

	private static final float XY_MOVEMENT_SPEED = 4.0f;
	public static final float ZOOM_SPEED = 10.0f;
	private final OrthographicCamera camera;
	private Vector2 xyVelocity = new Vector2();

	private float targetZoom = 2.0f;

	private static final float MAX_ZOOM = 10.0f;
	private static final float MIN_ZOOM = 0.3f;
	private int worldWidth;
	private int worldHeight;

	public PrimaryCameraManager() {
		camera = new OrthographicCamera();
		// TODO Get hold of actual window size and deal with resize events
		camera.setToOrtho(false, Gdx.graphics.getWidth() / 100.0f, Gdx.graphics.getHeight() / 100.0f);
		camera.zoom = 2.0f;
	}

	public void zoom(int zoomAmount) {
		targetZoom = camera.zoom + (camera.zoom * zoomAmount * 0.5f);
		if (targetZoom < MIN_ZOOM) {
			targetZoom = MIN_ZOOM;
		} else if (targetZoom > MAX_ZOOM) {
			targetZoom = MAX_ZOOM;
		}
	}

	public void update(float deltaSeconds) {
		camera.position.x += (xyVelocity.x * deltaSeconds * XY_MOVEMENT_SPEED * camera.zoom);
		if (camera.position.x < 0) {
			camera.position.x = 0;
		} else if (camera.position.x > worldWidth) {
			camera.position.x = worldWidth;
		}

		camera.position.y += (xyVelocity.y * deltaSeconds * XY_MOVEMENT_SPEED * camera.zoom);
		if (camera.position.y < 0) {
			camera.position.y = 0;
		} else if (camera.position.y > worldHeight) {
			camera.position.y = worldHeight;
		}

		if (camera.zoom < targetZoom) {
			float difference = targetZoom - camera.zoom;
			if (difference < 0.05f) {
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

		camera.update();
	}

	public void init(AbstractGameMap gameWorld) {
		this.worldWidth = gameWorld.getWidth();
		this.worldHeight = gameWorld.getHeight();
		camera.position.x = worldWidth / 2;
		camera.position.y = worldHeight / 2;
	}

	public OrthographicCamera getCamera() {
		return camera;
	}

	public void setMovementX(float amount) {
		xyVelocity.x = amount;
	}

	public void setMovementY(float amount) {
		xyVelocity.y = amount;
	}

	public void onResize(int width, int height) {
		float tempX = camera.position.x;
		float tempY = camera.position.y;
		float zoom = camera.zoom;
		camera.setToOrtho(false, width / 100.0f, height / 100.0f);
		camera.position.x = tempX;
		camera.position.y = tempY;
		camera.zoom = zoom;
	}
}
