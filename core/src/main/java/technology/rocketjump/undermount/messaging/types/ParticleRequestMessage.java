package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.Optional;

public class ParticleRequestMessage {

	public final String typeName;
	public ParticleEffectType type;
	public final Optional<Entity> parentEntity;
	public final Optional<JobTarget> effectTarget;
	public final ParticleCreationCallback callback;
	public final Optional<MapTile> parentTile;
	private Color overrideColor;

	public ParticleRequestMessage(ParticleEffectType type, Optional<Entity> parentEntity, Optional<JobTarget> effectTarget, ParticleCreationCallback callback) {
		this.typeName = null;
		this.type = type;
		this.parentEntity = parentEntity;
		this.effectTarget = effectTarget;
		this.callback = callback;
		this.parentTile = Optional.empty();
	}

	public ParticleRequestMessage(String typeName, Optional<Entity> parentEntity, Optional<JobTarget> effectTarget, ParticleCreationCallback callback) {
		this.typeName = typeName;
		this.type = null;
		this.parentEntity = parentEntity;
		this.effectTarget = effectTarget;
		this.callback = callback;
		this.parentTile = Optional.empty();
	}

	public Color getOverrideColor() {
		return overrideColor;
	}

	public void setOverrideColor(Color overrideColor) {
		this.overrideColor = overrideColor;
	}

	public interface ParticleCreationCallback {

		void particleCreated(ParticleEffectInstance instance);

	}
}
