package technology.rocketjump.undermount.rendering.custom_libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

public class VertexColorOnlyBatch implements Disposable {

	private static final int SPRITE_SIZE = 4 * (2 + 1); // Sprite.SPRITE_SIZE = 4 * VERTEX_SIZE

	/**
	 * Draws batched quads using indices.
	 *
	 * @author mzechner
	 * @author Nathan Sweet
	 * @see Batch
	 */
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
	private boolean ownsShader;

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

	public VertexColorOnlyBatch() {
		int size = 1000;

		// 32767 is max index, so 32767 / 6 - (32767 / 6 % 3) = 5460.
		if (size > 5460) throw new IllegalArgumentException("Can't have more than 5460 sprites per batch: " + size);

		mesh = new Mesh(Mesh.VertexDataType.VertexArray, false, size * 4, size * 6,
				new VertexAttribute(VertexAttributes.Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
				new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));

		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		vertices = new float[size * SPRITE_SIZE];

		int indicesLength = size * 6;
		short[] indices = new short[indicesLength];
		short j = 0;
		for (int i = 0; i < indicesLength; i += 6, j += 4) {
			indices[i] = j;
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);
			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 3);
			indices[i + 5] = j;
		}
		mesh.setIndices(indices);

		FileHandle vertexShaderFile = Gdx.files.classpath("shaders/vertex_color_only_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/vertex_color_only_fragment_shader.glsl");
		shader = ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);
		ownsShader = true;
	}

	public void begin() {
		if (drawing) throw new IllegalStateException("SpriteBatch.end must be called before begin.");
		renderCalls = 0;

		Gdx.gl.glDepthMask(false);
		shader.begin();
		setupMatrices();

		drawing = true;
	}

	public void end() {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before end.");
		if (idx > 0) flush();
		drawing = false;

		GL20 gl = Gdx.gl;
		gl.glDepthMask(true);
		gl.glDisable(GL20.GL_BLEND);
		shader.end();
	}

	public void draw(float x, float y, float width, float height, Color[] vertexColors) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (idx == vertices.length) //
			flush();

		final float fx2 = x + width;
		final float fy2 = y + height;

		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = vertexColors[0].toFloatBits();

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = vertexColors[1].toFloatBits();

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = vertexColors[2].toFloatBits();

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = vertexColors[3].toFloatBits();
	}

	public void flush() {
		if (idx == 0) return;

		renderCalls++;
		totalRenderCalls++;
		int spritesInBatch = idx / SPRITE_SIZE;
		if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
		int count = spritesInBatch * 6;

		Mesh mesh = this.mesh;
		mesh.setVertices(vertices, 0, idx);
		mesh.getIndicesBuffer().position(0);
		mesh.getIndicesBuffer().limit(count);

		// RT - Removed if on blendingDisabled
		Gdx.gl.glEnable(GL20.GL_BLEND);
		if (blendSrcFunc != -1) Gdx.gl.glBlendFunc(blendSrcFunc, blendDstFunc);


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

	public void dispose() {
		mesh.dispose();
		if (ownsShader && shader != null) shader.dispose();
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
		shader.setUniformi("u_texture0", 0);
		shader.setUniformi("u_texture1", 1);
	}

	public void setShader(ShaderProgram shader) {
		if (drawing) {
			flush();
			this.shader.end();
			this.shader.begin();
			setupMatrices();
		}
	}

	public boolean isDrawing() {
		return drawing;
	}
}

