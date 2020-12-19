package technology.rocketjump.undermount.rendering.custom_libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.NumberUtils;

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

public class AlphaMaskSpriteBatch implements Disposable {

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
	Texture colorTexture = null;
	Texture alphaTexture = null;
	float invColorTexWidth = 0, invAlphaTexWidth = 0,
			invColorTexHeight = 0, invAlphaTexHeight = 0;

	boolean drawing = false;

	private final Matrix4 transformMatrix = new Matrix4();
	private final Matrix4 projectionMatrix = new Matrix4();
	private final Matrix4 combinedMatrix = new Matrix4();

	private int blendSrcFunc = GL20.GL_SRC_ALPHA;
	private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;

	private final ShaderProgram shader;
	private boolean ownsShader;

	float color = Color.WHITE.toFloatBits();
	private Color tempColor = new Color(1, 1, 1, 1);
	private Color[] vertexColors = new Color[4];

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
	 * Constructs a new AlphaMaskSpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point upwards, x-axis
	 * point to the right and the origin being in the bottom left corner of the screen. The projection will be pixel perfect with
	 * respect to the current screen resolution.
	 * <p>
	 * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
	 * the ones expect for shaders set with {@link #setShader(ShaderProgram)}
	 *
	 */
	public AlphaMaskSpriteBatch() {
		int size = 1000;

		// 32767 is max index, so 32767 / 6 - (32767 / 6 % 3) = 5460.
		if (size > 5460) throw new IllegalArgumentException("Can't have more than 5460 sprites per batch: " + size);

		mesh = new Mesh(Mesh.VertexDataType.VertexArray, false, size * 4, size * 6, new VertexAttribute(VertexAttributes.Usage.Position, 2,
				ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
				new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "1")); // RT - This line added for texCoord1

		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		final int SPRITE_SIZE = 4 * (2 + 1 + 2 + 2); // Sprite.SPRITE_SIZE = 4 * VERTEX_SIZE = 4 * 2 + 1 + 2, though in our case there are 2 more
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

		FileHandle vertexShaderFile = Gdx.files.classpath("shaders/alpha_mask_blending_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/alpha_mask_blending_fragment_shader.glsl");
		shader = ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile);
		ownsShader = true;
	}

	protected void setTextures(Texture colorTexture, Texture alphaTexture) {
		flush();
		this.colorTexture = colorTexture;
		this.alphaTexture = alphaTexture;
		invColorTexWidth = 1.0f / colorTexture.getWidth();
		invColorTexHeight = 1.0f / colorTexture.getHeight();
		invAlphaTexWidth = 1.0f / alphaTexture.getWidth();
		invAlphaTexHeight = 1.0f / alphaTexture.getHeight();
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

	public void setColor(Color tint) {
		color = tint.toFloatBits();
	}

	public void setColor(float r, float g, float b, float a) {
		int intBits = (int) (255 * a) << 24 | (int) (255 * b) << 16 | (int) (255 * g) << 8 | (int) (255 * r);
		color = NumberUtils.intToFloatColor(intBits);
	}

	public void setColor(float color) {
		this.color = color;
	}

	public Color getColor() {
		int intBits = NumberUtils.floatToIntColor(color);
		Color color = tempColor;
		color.r = (intBits & 0xff) / 255f;
		color.g = ((intBits >>> 8) & 0xff) / 255f;
		color.b = ((intBits >>> 16) & 0xff) / 255f;
		color.a = ((intBits >>> 24) & 0xff) / 255f;
		return color;
	}

	public float getPackedColor() {
		return color;
	}

	public void draw(Sprite colorSprite, Sprite alphaSprite, float x, float y, float width, float height, Color color) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (colorSprite.getTexture() != colorTexture || alphaSprite.getTexture() != alphaTexture) {
			setTextures(colorSprite.getTexture(), alphaSprite.getTexture());
		}

		if (idx == vertices.length) //
			flush();

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float color_u = colorSprite.getU();
		final float color_v = colorSprite.getV2();
		final float color_u2 = colorSprite.getU2();
		final float color_v2 = colorSprite.getV();
		final float alpha_u = alphaSprite.getU();
		final float alpha_v = alphaSprite.getV2();
		final float alpha_u2 = alphaSprite.getU2();
		final float alpha_v2 = alphaSprite.getV();

		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = color.toFloatBits();
		vertices[idx++] = color_u;
		vertices[idx++] = color_v;
		vertices[idx++] = alpha_u;
		vertices[idx++] = alpha_v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = color.toFloatBits();
		vertices[idx++] = color_u;
		vertices[idx++] = color_v2;
		vertices[idx++] = alpha_u;
		vertices[idx++] = alpha_v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = color.toFloatBits();
		vertices[idx++] = color_u2;
		vertices[idx++] = color_v2;
		vertices[idx++] = alpha_u2;
		vertices[idx++] = alpha_v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = color.toFloatBits();
		vertices[idx++] = color_u2;
		vertices[idx++] = color_v;
		vertices[idx++] = alpha_u2;
		vertices[idx++] = alpha_v;
	}

	public void draw(Sprite colorSprite, Sprite alphaSprite, float tileX, float tileY, float offsetX, float offsetY, float width, float height) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (colorSprite.getTexture() != colorTexture || alphaSprite.getTexture() != alphaTexture) {
			setTextures(colorSprite.getTexture(), alphaSprite.getTexture());
		}

		if (idx == vertices.length) //
			flush();

		final float fx = tileX + offsetX;
		final float fy = tileY + offsetY;
		final float fx2 = fx + width;
		final float fy2 = fy + height;

		final float color_u = (colorSprite.getRegionX() + (offsetX * colorSprite.getRegionWidth())) * invColorTexWidth;
		final float color_v = (colorSprite.getRegionY() + ((0.5f - offsetY) * colorSprite.getRegionHeight())) * invColorTexHeight;
		final float color_u2 = color_u + (colorSprite.getRegionWidth() * width * invColorTexWidth);
		final float color_v2 = color_v + (colorSprite.getRegionHeight() * height * invColorTexHeight);

		final float alpha_u = (alphaSprite.getRegionX() + (offsetX * alphaSprite.getRegionWidth())) * invAlphaTexWidth;
		final float alpha_v = (alphaSprite.getRegionY() + ((0.5f - offsetY) * alphaSprite.getRegionHeight())) * invAlphaTexHeight;
		final float alpha_u2 = alpha_u + (alphaSprite.getRegionWidth() * width * invAlphaTexWidth);
		final float alpha_v2 = alpha_v + (alphaSprite.getRegionHeight() * height * invAlphaTexHeight);

		float vCol0 = vertexColors[0].toFloatBits(); // SW
		float vCol1 = vertexColors[1].toFloatBits(); // NW
		float vCol2 = vertexColors[2].toFloatBits(); // NE
		float vCol3 = vertexColors[3].toFloatBits(); // SE

		if (!vertexColors[0].equals(vertexColors[1]) || !vertexColors[2].equals(vertexColors[3])) {
			// skip this when colors the same
			if (offsetX > 0 && offsetY > 0) {
				vCol0 = vertexColors[0].cpy().lerp(vertexColors[1], 0.5f).lerp(vertexColors[2].cpy().lerp(vertexColors[3], 0.5f), 0.5f).toFloatBits();
				vCol1 = vertexColors[1].cpy().lerp(vertexColors[2], 0.5f).toFloatBits();
				vCol3 = vertexColors[2].cpy().lerp(vertexColors[3], 0.5f).toFloatBits();
			} else if (offsetX > 0) {
				vCol0 = vertexColors[0].cpy().lerp(vertexColors[3], 0.5f).toFloatBits();
				vCol2 = vertexColors[1].cpy().lerp(vertexColors[2], 0.5f).toFloatBits();
			} else if (offsetY > 0) {
				vCol0 = vertexColors[0].cpy().lerp(vertexColors[1], 0.5f).toFloatBits();
				vCol3 = vertexColors[2].cpy().lerp(vertexColors[3], 0.5f).toFloatBits();
			}
		}

		vertices[idx++] = fx;
		vertices[idx++] = fy;
		vertices[idx++] = vCol0;
		vertices[idx++] = color_u;
		vertices[idx++] = color_v2;
		vertices[idx++] = alpha_u;
		vertices[idx++] = alpha_v2;

		vertices[idx++] = fx;
		vertices[idx++] = fy2;
		vertices[idx++] = vCol1;
		vertices[idx++] = color_u;
		vertices[idx++] = color_v;
		vertices[idx++] = alpha_u;
		vertices[idx++] = alpha_v;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = vCol2;
		vertices[idx++] = color_u2;
		vertices[idx++] = color_v;
		vertices[idx++] = alpha_u2;
		vertices[idx++] = alpha_v;

		vertices[idx++] = fx2;
		vertices[idx++] = fy;
		vertices[idx++] = vCol3;
		vertices[idx++] = color_u2;
		vertices[idx++] = color_v2;
		vertices[idx++] = alpha_u2;
		vertices[idx++] = alpha_v2;
	}

	public void draw(Sprite colorSprite, Sprite alphaSprite, float x, float y, float width, float height) {
		if (!drawing) throw new IllegalStateException("SpriteBatch.begin must be called before draw.");

		float[] vertices = this.vertices;

		if (colorSprite.getTexture() != colorTexture || alphaSprite.getTexture() != alphaTexture) {
			setTextures(colorSprite.getTexture(), alphaSprite.getTexture());
		}

		if (idx == vertices.length) //
			flush();

		final float fx2 = x + width;
		final float fy2 = y + height;
		final float color_u = colorSprite.getU();
		final float color_v = colorSprite.getV2();
		final float color_u2 = colorSprite.getU2();
		final float color_v2 = colorSprite.getV();
		final float alpha_u = alphaSprite.getU();
		final float alpha_v = alphaSprite.getV2();
		final float alpha_u2 = alphaSprite.getU2();
		final float alpha_v2 = alphaSprite.getV();

		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = vertexColors[0].toFloatBits();
		vertices[idx++] = color_u;
		vertices[idx++] = color_v;
		vertices[idx++] = alpha_u;
		vertices[idx++] = alpha_v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = vertexColors[1].toFloatBits();
		vertices[idx++] = color_u;
		vertices[idx++] = color_v2;
		vertices[idx++] = alpha_u;
		vertices[idx++] = alpha_v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = vertexColors[2].toFloatBits();
		vertices[idx++] = color_u2;
		vertices[idx++] = color_v2;
		vertices[idx++] = alpha_u2;
		vertices[idx++] = alpha_v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = vertexColors[3].toFloatBits();
		vertices[idx++] = color_u2;
		vertices[idx++] = color_v;
		vertices[idx++] = alpha_u2;
		vertices[idx++] = alpha_v;
	}

	public void flush() {
		if (idx == 0) return;

		renderCalls++;
		totalRenderCalls++;
		int spritesInBatch = idx / 28;
		if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
		int count = spritesInBatch * 6;

		alphaTexture.bind(1);
		colorTexture.bind(0);
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
		if (colorTexture != null) {
			colorTexture.dispose();
			colorTexture = null;
		}
		if (alphaTexture != null) {
			alphaTexture.dispose();
			alphaTexture = null;
		}
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

	public void setColors(Color[] vertexColors) {
		this.vertexColors = vertexColors;
	}

	public boolean isDrawing() {
		return drawing;
	}
}

