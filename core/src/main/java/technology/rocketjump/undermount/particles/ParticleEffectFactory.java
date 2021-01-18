package technology.rocketjump.undermount.particles;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.particles.custom_libgdx.ParticleEffect;
import technology.rocketjump.undermount.particles.custom_libgdx.ParticleEmitter;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.rendering.RenderMode;

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
			if (pfile.exists()) {
				Logger.error("Could not find pfile with name " + particleEffectType.getPFile() + " in assets/definitions/particles");
				continue;
			}

			ParticleEffect baseInstance = new ParticleEffect(particleEffectType.getIsAffectedByLighting());
			baseInstance.load(pfile, diffuseTextureAtlas, normalTextureAtlas, null);
//			baseInstance.loadEmitters(pfile);
//			baseInstance.loadEmitterImages(diffuseTextureAtlas, normalTextureAtlas);
			baseInstance.draw(null, RenderMode.DIFFUSE);

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
