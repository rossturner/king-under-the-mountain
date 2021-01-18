/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package technology.rocketjump.undermount.particles.custom_libgdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import technology.rocketjump.undermount.rendering.RenderMode;

import java.io.*;
import java.util.Random;

/** See <a href="http://www.badlogicgames.com/wordpress/?p=1255">http://www.badlogicgames.com/wordpress/?p=1255</a>
 * @author mzechner */
public class ParticleEffect implements Disposable {
	private final Array<ParticleEmitter> emitters;
	private BoundingBox bounds;
	private boolean ownsTexture;
	protected float xSizeScale = 1f;
	protected float ySizeScale = 1f;
	protected float motionScale = 1f;
	private boolean usesNormalsRendering = false;
	private Random random;

	public ParticleEffect(boolean isAffectedByLighting) {
		emitters = new Array(8);
		random = new RandomXS128();
		this.usesNormalsRendering = isAffectedByLighting;
	}

	public ParticleEffect (ParticleEffect effect) {
		this.usesNormalsRendering = effect.usesNormalsRendering;
		this.random = new RandomXS128(); // every ParticleEffect instance has a new random/seed, not same as cloned base
		emitters = new Array(true, effect.emitters.size);
		for (int i = 0, n = effect.emitters.size; i < n; i++)
			emitters.add(newEmitter(effect.emitters.get(i)));
	}

	public void start () {
		for (int i = 0, n = emitters.size; i < n; i++)
			emitters.get(i).start();
	}

	/** Resets the effect so it can be started again like a new effect. Any changes to 
	 * scale are reverted. See {@link #reset(boolean)}.*/
	public void reset () {
		reset(true);
	}
	
	/** Resets the effect so it can be started again like a new effect.
	 * @param resetScaling Whether to restore the original size and motion parameters if they were scaled. Repeated scaling
	 * and resetting may introduce error. */
	public void reset (boolean resetScaling){
		for (int i = 0, n = emitters.size; i < n; i++)
			emitters.get(i).reset();
		if (resetScaling && (xSizeScale != 1f || ySizeScale != 1f || motionScale != 1f)){
			scaleEffect(1f / xSizeScale, 1f / ySizeScale, 1f / motionScale);
			xSizeScale = ySizeScale = motionScale = 1f;
		}
	}

	public void update (float delta) {
		for (int i = 0, n = emitters.size; i < n; i++)
			emitters.get(i).update(delta);
	}

	public void draw(Batch spriteBatch, RenderMode renderMode) {
		for (int i = 0, n = emitters.size; i < n; i++)
			emitters.get(i).draw(spriteBatch, renderMode);
	}

	public void allowCompletion () {
		for (int i = 0, n = emitters.size; i < n; i++)
			emitters.get(i).allowCompletion();
	}

	public boolean isComplete () {
		for (int i = 0, n = emitters.size; i < n; i++) {
			ParticleEmitter emitter = emitters.get(i);
			if (!emitter.isComplete()) return false;
		}
		return true;
	}

	public void setDuration (int duration) {
		for (int i = 0, n = emitters.size; i < n; i++) {
			ParticleEmitter emitter = emitters.get(i);
			emitter.setContinuous(false);
			emitter.duration = duration;
			emitter.durationTimer = 0;
		}
	}

	public void setPosition (float x, float y) {
		for (int i = 0, n = emitters.size; i < n; i++)
			emitters.get(i).setPosition(x, y);
	}

	public void setFlip (boolean flipX, boolean flipY) {
		for (int i = 0, n = emitters.size; i < n; i++)
			emitters.get(i).setFlip(flipX, flipY);
	}

	public void flipY () {
		for (int i = 0, n = emitters.size; i < n; i++)
			emitters.get(i).flipY();
	}

	public Array<ParticleEmitter> getEmitters () {
		return emitters;
	}

	/** Returns the emitter with the specified name, or null. */
	public ParticleEmitter findEmitter (String name) {
		for (int i = 0, n = emitters.size; i < n; i++) {
			ParticleEmitter emitter = emitters.get(i);
			if (emitter.getName().equals(name)) return emitter;
		}
		return null;
	}

	public void save (Writer output) throws IOException {
		int index = 0;
		for (int i = 0, n = emitters.size; i < n; i++) {
			ParticleEmitter emitter = emitters.get(i);
			if (index++ > 0) output.write("\n");
			emitter.save(output);
		}
	}

	public void load (FileHandle effectFile, TextureAtlas diffuseAtlas, TextureAtlas normalsAtlas, String atlasPrefix) {
		loadEmitters(effectFile);
		loadEmitterImages(diffuseAtlas, normalsAtlas, atlasPrefix);
	}

	public void loadEmitters (FileHandle effectFile) {
		InputStream input = effectFile.read();
		emitters.clear();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(input), 512);
			while (true) {
				ParticleEmitter emitter = newEmitter(reader);
				emitters.add(emitter);
				if (reader.readLine() == null) break;
			}
		} catch (IOException ex) {
			throw new GdxRuntimeException("Error loading effect: " + effectFile, ex);
		} finally {
			StreamUtils.closeQuietly(reader);
		}
	}

	public void loadEmitterImages(TextureAtlas diffuseAtlas, TextureAtlas normalsAtlas) {
		loadEmitterImages(diffuseAtlas, normalsAtlas,null);
	}

	public void loadEmitterImages(TextureAtlas diffuseAtlas, TextureAtlas normalsAtlas, String atlasPrefix) {
		for (int i = 0, n = emitters.size; i < n; i++) {
			ParticleEmitter emitter = emitters.get(i);
			if (emitter.getImagePaths().size == 0) continue;
			Array<Sprite> diffuseSprites = new Array<Sprite>();
			Array<Sprite> normalsSprites = new Array<Sprite>();
			for (String imagePath : emitter.getImagePaths()) {
				String imageName = new File(imagePath.replace('\\', '/')).getName();
				int lastDotIndex = imageName.lastIndexOf('.');
				if (lastDotIndex != -1) imageName = imageName.substring(0, lastDotIndex);
				if (atlasPrefix != null) imageName = atlasPrefix + imageName;
				Sprite diffuseSprite = diffuseAtlas.createSprite(imageName);
				if (diffuseSprite == null) throw new IllegalArgumentException("SpriteSheet missing diffuse image: " + imageName);
				diffuseSprites.add(diffuseSprite);
				if (usesNormalsRendering) {
					Sprite normalsSprite = normalsAtlas.createSprite(imageName);
					if (normalsSprite == null) throw new IllegalArgumentException("SpriteSheet missing normals image: " + imageName);
					normalsSprites.add(normalsSprite);
				}
			}
			emitter.setSprites(diffuseSprites, normalsSprites);
		}
	}

	protected ParticleEmitter newEmitter (BufferedReader reader) throws IOException {
		return new ParticleEmitter(reader, usesNormalsRendering);
	}

	protected ParticleEmitter newEmitter (ParticleEmitter emitter) {
		return new ParticleEmitter(emitter);
	}

	/** Disposes the texture for each sprite for each ParticleEmitter. */
	public void dispose () {
		if (!ownsTexture) return;
		for (int i = 0, n = emitters.size; i < n; i++) {
			ParticleEmitter emitter = emitters.get(i);
			for (Sprite sprite : emitter.getDiffuseSprites()) {
				sprite.getTexture().dispose();
			}
			if (usesNormalsRendering) {
				for (Sprite sprite : emitter.getNormalSprites()) {
					sprite.getTexture().dispose();
				}
			}
		}
	}

	/** Returns the bounding box for all active particles. z axis will always be zero. */
	public BoundingBox getBoundingBox () {
		if (bounds == null) bounds = new BoundingBox();

		BoundingBox bounds = this.bounds;
		bounds.inf();
		for (ParticleEmitter emitter : this.emitters)
			bounds.ext(emitter.getBoundingBox());
		return bounds;
	}

	/** Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a
	 * {@link ParticleEffectPool}, the scale will be reset when it is returned to the pool. */
	public void scaleEffect (float scaleFactor) {
		scaleEffect(scaleFactor, scaleFactor, scaleFactor);
	}
	
	/** Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a
	 * {@link ParticleEffectPool}, the scale will be reset when it is returned to the pool. */
	public void scaleEffect (float scaleFactor, float motionScaleFactor) {
		scaleEffect(scaleFactor, scaleFactor, motionScaleFactor);
	}

	/** Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a
	 * {@link ParticleEffectPool}, the scale will be reset when it is returned to the pool. */
	public void scaleEffect (float xSizeScaleFactor, float ySizeScaleFactor, float motionScaleFactor) {
		xSizeScale *= xSizeScaleFactor;
		ySizeScale *= ySizeScaleFactor;
		motionScale *= motionScaleFactor;
		for (ParticleEmitter particleEmitter : emitters) {
			particleEmitter.scaleSize(xSizeScaleFactor, ySizeScaleFactor);
			particleEmitter.scaleMotion(motionScaleFactor);
		}
	}

	/** Sets the {@link ParticleEmitter#setCleansUpBlendFunction(boolean) cleansUpBlendFunction}
	 * parameter on all {@link ParticleEmitter ParticleEmitters} currently in this ParticleEffect.
	 * <p>
	 * IMPORTANT: If set to false and if the next object to use this Batch expects alpha blending, you are responsible for setting
	 * the Batch's blend function to (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA) before that next object is drawn.
	 * @param cleanUpBlendFunction */
	public void setEmittersCleanUpBlendFunction (boolean cleanUpBlendFunction) {
		for (int i = 0, n = emitters.size; i < n; i++) {
			emitters.get(i).setCleansUpBlendFunction(cleanUpBlendFunction);
		}
	}
}
