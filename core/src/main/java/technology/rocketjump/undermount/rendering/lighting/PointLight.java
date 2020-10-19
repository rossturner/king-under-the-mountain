package technology.rocketjump.undermount.rendering.lighting;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.undermount.mapping.tile.wall.Edge;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public class PointLight implements ChildPersistable, Disposable {

	private final PointLightMesh mesh;
	private Vector2 worldPosition = new Vector2();
	public static float LIGHT_RADIUS = 7.0f;
	private Array<Edge> lightPolygonEdges = new Array<>(); // These are the pairs of points other than the origin which make up the visibility polygon
	private Color color = Color.WHITE;

	public PointLight() {
		this.mesh = new PointLightMesh();
	}

	public PointLight(PointLightMesh pointLightMesh) {
		this.mesh = pointLightMesh;
	}

	public Vector2 getWorldPosition() {
		return worldPosition;
	}

	public void setWorldPosition(Vector2 worldPosition) {
		this.worldPosition = worldPosition;
	}

	public Array<Edge> getLightPolygonEdges() {
		return lightPolygonEdges;
	}

	public void setLightPolygonEdges(Array<Edge> lightPolygonEdges) {
		this.lightPolygonEdges = lightPolygonEdges;
	}

	public void updateMesh() {
		mesh.updateGeometry(worldPosition, LIGHT_RADIUS, lightPolygonEdges);
	}

	public void render(Camera camera, ShaderProgram lightShader) {
		mesh.render(camera, color, lightShader);
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!color.equals(Color.WHITE)) {
			asJson.put("color", HexColors.toHexString(color));
		}
		asJson.put("worldPosition", JSONUtils.toJSON(worldPosition));

		// mesh and light polygon edges are update dynamically
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.color = HexColors.get(asJson.getString("color"));
		if (this.color == null) {
			this.color = Color.WHITE;
		}
		this.worldPosition = JSONUtils.vector2(asJson.getJSONObject("worldPosition"));
	}

	@Override
	public void dispose() {
		mesh.dispose();
	}
}
