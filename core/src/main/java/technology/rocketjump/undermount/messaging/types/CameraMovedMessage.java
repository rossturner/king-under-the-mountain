package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraMovedMessage {

	public final Vector2 cursorWorldPosition;
	public final float viewportTileWidth;
	public final float viewportTileHeight;
	public final Vector3 cameraPosition;
	public final float minTilesForZoom;
	public final float maxTilesForZoom;

	public CameraMovedMessage(float viewportTileWidth, float viewportTileHeight, Vector3 cameraPosition, Vector2 cursorWorldPosition,
							  float minTilesForZoom, float maxTilesForZoom) {
		this.viewportTileWidth = viewportTileWidth;
		this.viewportTileHeight = viewportTileHeight;
		this.cameraPosition = cameraPosition;
		this.cursorWorldPosition = cursorWorldPosition;
		this.minTilesForZoom = minTilesForZoom;
		this.maxTilesForZoom = maxTilesForZoom;
	}

}
