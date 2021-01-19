package technology.rocketjump.undermount.particles;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.particles.custom_libgdx.ParticleEffect;
import technology.rocketjump.undermount.particles.custom_libgdx.ParticleEmitter;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES;
import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES;

@Singleton
public class ParticleEffectFactory {

	private final ParticleEffectTypeDictionary typeDictionary;

	private final Map<ParticleEffectType, ParticleEffect> baseInstancesByDefinition = new HashMap<>();

	@Inject
	public ParticleEffectFactory(ParticleEffectTypeDictionary typeDictionary, TextureAtlasRepository textureAtlasRepository) {
		this.typeDictionary = typeDictionary;

		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(NORMAL_ENTITIES);

		for (ParticleEffectType particleEffectType : typeDictionary.getAll()) {

			// load from pfile
			FileHandle pfile = new FileHandle("assets/definitions/particles/" + particleEffectType.getPFile());
			if (!pfile.exists()) {
				Logger.error("Could not find pfile with name " + particleEffectType.getPFile() + " in assets/definitions/particles");
				continue;
			}

			ParticleEffect baseInstance = new ParticleEffect(particleEffectType.getIsAffectedByLighting());
			baseInstance.scaleEffect(1f/(64f * 64f));
			baseInstance.load(pfile, diffuseTextureAtlas, normalTextureAtlas, null);


			baseInstancesByDefinition.put(particleEffectType, baseInstance);
		}
	}

	public ParticleEffectInstance create(ParticleEffectType type, Entity parentEntity, Optional<GameMaterial> optionalMaterial) {

		ParticleEffect gdxBaseInstance = baseInstancesByDefinition.get(type);
		ParticleEffect gdxClone = new ParticleEffect(gdxBaseInstance);
		if (optionalMaterial.isPresent()) {
			float[] colors = new float[] {optionalMaterial.get().getColor().toFloatBits(), optionalMaterial.get().getColor().toFloatBits(), optionalMaterial.get().getColor().toFloatBits()};
			for (ParticleEmitter emitter : gdxClone.getEmitters()) {
				emitter.getTint().setColors(colors);
			}
		}

		ParticleEffectInstance instance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), type, gdxClone, Optional.of(parentEntity));
		instance.setWorldPosition(parentEntity.getLocationComponent().getWorldOrParentPosition());

		Vector2 offset = parentEntity.getLocationComponent().getOrientation().toVector2().cpy().scl(type.getDistanceFromParentEntityOrientation());
		instance.setOffsetFromWorldPosition(offset);

		gdxClone.start();

		return instance;
	}

}
