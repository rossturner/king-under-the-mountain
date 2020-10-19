package technology.rocketjump.undermount.rendering.lighting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.NumberUtils;


/**
 * Draws batched quads of lighting color and amount
 * Based on LibGDX SpriteBatch
 *
 * @see SpriteBatch
 */
public class AmbientLightingBatch implements Disposable {

	private static final int NUM_VERTICES_PER_QUAD = 4;
	private static final int NUM_INDICES_PER_QUAD = 6;
	private static final int VERTEX_SIZE = 2 + 1;
	private static final int SPRITE_SIZE = NUM_VERTICES_PER_QUAD * VERTEX_SIZE;
	private Mesh mesh;

	final float[] vertices;
	int idx = 0;

	boolean drawing = false;

	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();

	private int blendSrcFunc = GL20.GL_SRC_ALPHA;
	private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;

	private final ShaderProgram shader;

	Color color = Color.WHITE;

	/**
	 * Number of render calls since the last {@link #begin()}.
	 **/
	public int renderCalls = 0;

	/**
	 * Number of rendering calls, ever. Will not be reset unless set manually.
	 **/
	public int totalRenderCalls = 0;

	/**
	 * The maximum number of sprites rendered in one batch so far.
	 **/
	public int maxSpritesInBatch = 0;

	/**
	 * Constructs a new SpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
	 * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
	 * respect to the current screen resolution.
	 *
	 * @param maxNumQuads The max number of sprites in a single batch. Max of 5460.
	 * @param shader      The default shader to use. This is not owned by the SpriteBatch and must be disposed separately.
	 */
	public AmbientLightingBatch(int maxNumQuads, ShaderProgram shader) {
		// 32767 is max index, so 32767 / 6 - (32767 / 6 % 3) = 5460.
		if (maxNumQuads > 5460)
			throw new IllegalArgumentException("Can't have more than 5460 sprites per batch: " + maxNumQuads);

		Mesh.VertexDataType vertexDataType = Mesh.VertexDataType.VertexArray;
		if (Gdx.gl30 != null) {
			vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO;
		}
//		mesh = new Mesh(vertexDataType, false, size * 4, size * 6,
//				new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
//				new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
//				new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		mesh = new Mesh(vertexDataType, false, maxNumQuads * NUM_VERTICES_PER_QUAD, maxNumQuads * NUM_INDICES_PER_QUAD,
				new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
//				new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.Generic, 1, "a_luminosity"));

		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		vertices = new float[maxNumQuads * SPRITE_SIZE];

		int len = maxNumQuads * 6;
		short[] indices = new short[len];
		short j = 0;
		for (int i = 0; i < len; i += 6, j += 4) {
			indices[i] = j;
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);
			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 3);
			indices[i + 5] = j;
		}
		mesh.setIndices(indices);
		this.shader = shader;
	}

	public void begin() {
		if (drawing) throw new IllegalStateException("SpriteBatch.end must be called before begin.");
		renderCalls = 0;

		Gdx.gl.glDepthMask(false);
		shader.begin();
		shader.setUniformf("u_lightColor", new Vector3(color.r, color.g, color.b));
		setupMatrices();

		drawing = true;
	}

	public void end() {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before end.");
		if (idx > 0) flush();
		drawing = false;

		GL20 gl = Gdx.gl;
		gl.glDepthMask(true);

		shader.end();
	}

	public void setColor(Color tint) {
		this.color = tint;
	}

	public void draw(float x, float y, float width, float height, float luminositySW, float luminosityNW, float luminosityNE, float luminositySE) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		if (idx == vertices.length) {
			flush();
		}

		final float xPlusWidth = x + width;
		final float yPlusHeight = y + height;

		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = luminositySW;

		vertices[idx++] = x;
		vertices[idx++] = yPlusHeight;
		vertices[idx++] = luminosityNW;

		vertices[idx++] = xPlusWidth;
		vertices[idx++] = yPlusHeight;
		vertices[idx++] = luminosityNE;

		vertices[idx++] = xPlusWidth;
		vertices[idx++] = y;
		vertices[idx++] = luminositySE;
	}

	public void flush() {
		if (idx == 0) return;

		renderCalls++;
		totalRenderCalls++;
		int spritesInBatch = idx / (VERTEX_SIZE * NUM_VERTICES_PER_QUAD);
		if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
		int count = spritesInBatch * 6;

		Mesh mesh = this.mesh;
		mesh.setVertices(vertices, 0, idx);
		mesh.getIndicesBuffer().position(0);
		mesh.getIndicesBuffer().limit(count);

		Gdx.gl.glEnable(GL20.GL_BLEND);
		if (blendSrcFunc != -1) {
			Gdx.gl.glBlendFunc(blendSrcFunc, blendDstFunc);
		}

		mesh.render(shader, GL20.GL_TRIANGLES, 0, count);

		idx = 0;
	}

	public void setBlendFunction(int srcFunc, int dstFunc) {
		if (blendSrcFunc == srcFunc && blendDstFunc == dstFunc) return;
		flush();
		blendSrcFunc = srcFunc;
		blendDstFunc = dstFunc;
	}

	public int getBlendSrcFunc() {
		return blendSrcFunc;
	}

	public int getBlendDstFunc() {
		return blendDstFunc;
	}

	@Override
	public void dispose() {
		mesh.dispose();
	}

	public Matrix4 getProjectionMatrix() {
		return projectionMatrix;
	}

	public Matrix4 getTransformMatrix() {
		return transformMatrix;
	}

	public void setProjectionMatrix(Matrix4 projection) {
		if (drawing) flush();
		projectionMatrix.set(projection);
		if (drawing) setupMatrices();
	}

	public void setTransformMatrix(Matrix4 transform) {
		if (drawing) flush();
		transformMatrix.set(transform);
		if (drawing) setupMatrices();
	}

	private void setupMatrices() {
		combinedMatrix.set(projectionMatrix).mul(transformMatrix);

		shader.setUniformMatrix("u_projTrans", combinedMatrix);
//		shader.setUniformi("u_texture", 0);
	}

	public boolean isDrawing() {
		return drawing;
	}
}
