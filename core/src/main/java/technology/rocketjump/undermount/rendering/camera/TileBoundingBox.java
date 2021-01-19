package technology.rocketjump.undermount.rendering.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.mapping.model.TiledMap;

public class TileBoundingBox {

	public final int minX;
	public final int maxX;
	public final int minY;
	public final int maxY;


	public static int getMaxY(OrthographicCamera camera, TiledMap tiledMap) {
		int maxY = (int) Math.ceil(camera.frustum.planePoints[2].y);
		maxY += 2;
		if (maxY >= tiledMap.getHeight()) {
			maxY = tiledMap.getHeight() - 1;
		}
		return maxY;
	}

	public static int getMinY(OrthographicCamera camera) {
		int minY = (int) Math.floor(camera.frustum.planePoints[0].y);
		minY += -1;
		if (minY < 0) {
			minY = 0;
		}
		return minY;
	}

	public static int getMaxX(OrthographicCamera camera, TiledMap tiledMap) {
		int maxX = (int) Math.ceil(camera.frustum.planePoints[2].x);
		maxX += 2;
		if (maxX >= tiledMap.getWidth()) {
			maxX = tiledMap.getWidth() - 1;
		}
		return maxX;
	}

	public static int getMinX(OrthographicCamera camera) {
		int minX = (int) Math.floor(camera.frustum.planePoints[0].x);
		minX += -1;
		if (minX < 0) {
			minX = 0;
		}
		return minX;
	}

	public TileBoundingBox(OrthographicCamera camera, TiledMap map) {
		this.minX = getMinX(camera);
		this.maxX = getMaxX(camera, map);
		this.minY = getMinY(camera);
		this.maxY = getMaxY(camera, map);
	}

	public boolean contains(Vector2 position) {
		return minX < position.x && position.x < maxX && minY < position.y && position.y < maxY;
	}
}
