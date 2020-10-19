package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.undermount.mapping.tile.wall.Edge;

public class PointLightMesh implements Disposable {

	private Mesh internalMesh;
	private static int MAX_NUM_TRIANGLES = 2000;
	private static final int NUM_INDEX_PER_TRIANGLE = 3;
	private static final String LIGHT_POSITION_UNIFORM = "u_lightPosition";

//	private final Matrix4 transformMatrix = new Matrix4();

//	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();
	private final float[] vertices;
	private final short[] indices;
	private int vertexComponentIndex = 0;
	private final int VERTEX_SIZE = (2 + 2);


	private Vector2 worldPosition;

	public PointLightMesh() {
		final int MAX_VERTICES = MAX_NUM_TRIANGLES * VERTEX_SIZE * 3;
		vertices = new float[MAX_VERTICES];

		int MAX_INDICES = MAX_NUM_TRIANGLES * NUM_INDEX_PER_TRIANGLE;
		indices = new short[MAX_INDICES];

		internalMesh = new Mesh(Mesh.VertexDataType.VertexArray, false, MAX_VERTICES, MAX_INDICES,
				new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
//				new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_positionRelativeToLight")
			);

//		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		int vertexCursor = 0;
		for (int indexCursor = 0; indexCursor < MAX_INDICES; indexCursor += 3, vertexCursor += 3) {
			indices[indexCursor] = (short) (vertexCursor);
			indices[indexCursor + 1] = (short) (vertexCursor + 1);
			indices[indexCursor + 2] = (short) (vertexCursor + 2);
		}
		internalMesh.setIndices(indices);
	}


	public void updateGeometry(Vector2 worldPosition, float radius, Array<Edge> lightPolygonEdges) {
//		transformMatrix.setToTranslation(worldPosition.x, worldPosition.y, height);
		this.worldPosition = worldPosition;

		vertexComponentIndex = 0;

		for (Edge polygonEdge : lightPolygonEdges) {
			if (polygonEdge.getPointA() == null || polygonEdge.getPointB() == null) {
				// FIXME Can't see how this is happening but some null edges are creeping through sometimes
				continue;
			}
			vertices[vertexComponentIndex++] = worldPosition.x; // vertex X
			vertices[vertexComponentIndex++] = worldPosition.y; // vertex Y
			vertices[vertexComponentIndex++] = 0f; // x distance from light
			vertices[vertexComponentIndex++] = 0f; // y distance from light

			vertices[vertexComponentIndex++] = worldPosition.x + polygonEdge.getPointA().x; // vertex X
			vertices[vertexComponentIndex++] = worldPosition.y + polygonEdge.getPointA().y; // vertex Y
			vertices[vertexComponentIndex++] = polygonEdge.getPointA().x / radius; // x distance from light
			vertices[vertexComponentIndex++] = polygonEdge.getPointA().y / radius; // y distance from light

			vertices[vertexComponentIndex++] = worldPosition.x + polygonEdge.getPointB().x; // vertex X
			vertices[vertexComponentIndex++] = worldPosition.y + polygonEdge.getPointB().y; // vertex Y
			vertices[vertexComponentIndex++] = polygonEdge.getPointB().x / radius; // x distance from light
			vertices[vertexComponentIndex++] = polygonEdge.getPointB().y / radius; // y distance from light
		}

		internalMesh.setVertices(vertices, 0, vertexComponentIndex);
		internalMesh.getIndicesBuffer().position(0);
		internalMesh.getIndicesBuffer().limit(lightPolygonEdges.size * NUM_INDEX_PER_TRIANGLE);
	}

	public void render(Camera camera, Color color, ShaderProgram lightShader) {
		if (worldPosition == null) {
			// TODO Figure out why this is being called when worldPosition is null (if it still is)
			return;
		}

		Vector3 lightPosition = new Vector3(worldPosition.x, worldPosition.y, 0.5f).mul(camera.combined);
		lightPosition.z = 0.1f;
		lightShader.setUniformf(LIGHT_POSITION_UNIFORM, lightPosition);
		lightShader.setUniformf("u_lightColor", new Vector3(color.r, color.g, color.b));

		combinedMatrix.set(camera.combined);//.mul(camera.combined);//.mul(transformMatrix);
		lightShader.setUniformMatrix("u_projTrans", combinedMatrix);

		internalMesh.render(lightShader, GL20.GL_TRIANGLES, 0, internalMesh.getIndicesBuffer().limit());
	}

	@Override
	public void dispose() {
		internalMesh.dispose();
	}
}
