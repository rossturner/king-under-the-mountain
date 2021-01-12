package technology.rocketjump.undermount.particles;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ParticleEffectFactory {

	private final ParticleEffectTypeDictionary typeDictionary;

	private final Map<ParticleEffectType, ParticleEffect> baseInstancesByDefinition = new HashMap<>();

	@Inject
	public ParticleEffectFactory(ParticleEffectTypeDictionary typeDictionary) {
		this.typeDictionary = typeDictionary;

		for (ParticleEffectType particleEffectType : typeDictionary.getAll()) {
			ParticleEffect baseInstance = new ParticleEffect();

			baseInstance.draw();
		}
	}

	public ParticleEffectInstance create(ParticleEffectType type, Optional<GameMaterial> optionalMaterial) {

		ParticleEffect baseInstance = baseInstancesByDefinition.get(type);
		ParticleEffect gdxEffect = new ParticleEffect(baseInstance);
		if (optionalMaterial.isPresent()) {
			float[] colors = new float[] {optionalMaterial.get().getColor().toFloatBits(), optionalMaterial.get().getColor().toFloatBits(), optionalMaterial.get().getColor().toFloatBits()};
			for (ParticleEmitter emitter : gdxEffect.getEmitters()) {
				emitter.getTint().setColors(colors);
			}
		}
		return null;
	}

}
