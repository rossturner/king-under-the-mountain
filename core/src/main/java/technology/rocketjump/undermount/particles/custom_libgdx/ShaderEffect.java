package technology.rocketjump.undermount.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.rendering.RenderMode;
import technology.rocketjump.undermount.rendering.custom_libgdx.CustomShaderSpriteBatch;

public class ShaderEffect implements ParticleEffect {

	private final Vector2 worldPosition = new Vector2();
	private final ShaderProgram shader;
	private final ParticleEffectType particleEffectType;
	private long seed = new RandomXS128().nextLong();
	private boolean overrideCompletion;

	public ShaderEffect(ShaderProgram shader, ParticleEffectType particleEffectType) {
		this.shader = shader;
		this.particleEffectType = particleEffectType;
	}

	public ShaderEffect(ShaderEffect baseInstance) {
		this.shader = baseInstance.shader;
		this.particleEffectType = baseInstance.particleEffectType;
	}

	@Override
	public void update(float deltaTime) {

	}

	@Override
	public boolean isComplete() {
		return overrideCompletion;
	}

	@Override
	public void allowCompletion() {
		overrideCompletion = true;
	}

	@Override
	public void draw(SpriteBatch basicSpriteBatch, CustomShaderSpriteBatch customShaderSpriteBatch, RenderMode renderMode) {
		if (customShaderSpriteBatch == null) {
			Logger.warn(this.getClass().getSimpleName() + " currently does not support being affected by lighting");
			return;
		}
		if (customShaderSpriteBatch.getShader() != shader) {
			customShaderSpriteBatch.setShader(shader);
		}

		customShaderSpriteBatch.setSeed(seed);
		customShaderSpriteBatch.draw((Texture)null, worldPosition.x - (particleEffectType.getShaderEffectWidth() / 2f),
				worldPosition.y - (particleEffectType.getShaderEffectHeight() / 2f),
				particleEffectType.getShaderEffectWidth(), particleEffectType.getShaderEffectHeight());
	}

	@Override
	public void setPosition(float worldX, float worldY) {
		this.worldPosition.set(worldX, worldY);
	}

	@Override
	public void setTint(Color color) {

	}

	@Override
	public void adjustForParentOrientation(EntityAssetOrientation effectDefaultOrientation, EntityAssetOrientation parentOrientation) {
		// Do nothing
	}
}
