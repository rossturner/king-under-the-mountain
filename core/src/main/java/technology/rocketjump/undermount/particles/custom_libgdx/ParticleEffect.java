package technology.rocketjump.undermount.particles.custom_libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.rendering.RenderMode;

public interface ParticleEffect {

	void update(float deltaTime);

	boolean isComplete();

	void allowCompletion();

	void draw(Batch spriteBatch, RenderMode renderMode);

	void setPosition(float worldX, float worldY);

	void setTint(Color color);

	void adjustForParentOrientation(EntityAssetOrientation effectDefaultOrientation, EntityAssetOrientation parentOrientation);
}
