package technology.rocketjump.undermount.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.particles.custom_libgdx.LibgdxParticleEffect;
import technology.rocketjump.undermount.particles.custom_libgdx.ShaderEffect;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.rendering.custom_libgdx.ShaderLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES;
import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES;

@Singleton
public class ParticleEffectFactory {

	private static final String FILE_PREFIX = "assets/definitions/shaders/";
	private final ParticleEffectTypeDictionary typeDictionary;
	private final CustomEffectFactory customEffectFactory;

	private final Map<ParticleEffectType, LibgdxParticleEffect> baseInstancesByDefinition = new HashMap<>();
	private final Map<ParticleEffectType, ShaderEffect> shaderEffectBaseInstances = new HashMap<>();

	@Inject
	public ParticleEffectFactory(ParticleEffectTypeDictionary typeDictionary, TextureAtlasRepository textureAtlasRepository, CustomEffectFactory customEffectFactory) {
		this.typeDictionary = typeDictionary;
		this.customEffectFactory = customEffectFactory;

		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(NORMAL_ENTITIES);

		Sprite placeholderSprite = diffuseTextureAtlas.createSprite("placeholder");

		for (ParticleEffectType particleEffectType : typeDictionary.getAll()) {
			if (particleEffectType.getCustomImplementation() != null) {
				continue;
			} else if (particleEffectType.getFragmentShaderFile() != null && particleEffectType.getVertexShaderFile() != null) {
				ShaderProgram shaderProgram = ShaderLoader.createShader(Gdx.files.internal(FILE_PREFIX + particleEffectType.getVertexShaderFile()),
						Gdx.files.internal(FILE_PREFIX + particleEffectType.getFragmentShaderFile()));
				ShaderEffect shaderEffect = new ShaderEffect(shaderProgram, particleEffectType);
				shaderEffectBaseInstances.put(particleEffectType, shaderEffect);
			} else if (particleEffectType.getParticleFile() != null) {
				// load from pfile
				FileHandle pfile = new FileHandle("assets/definitions/particles/" + particleEffectType.getParticleFile());
				if (!pfile.exists()) {
					Logger.error("Could not find pfile with name " + particleEffectType.getParticleFile() + " in assets/definitions/particles");
					continue;
				}

				LibgdxParticleEffect baseInstance = new LibgdxParticleEffect(particleEffectType.getIsAffectedByLighting());
				baseInstance.load(pfile, diffuseTextureAtlas, normalTextureAtlas, null);
				baseInstance.scaleEffect((1f / 64f) * particleEffectType.getScale());

				baseInstancesByDefinition.put(particleEffectType, baseInstance);
			} else {
				Logger.error("Did not load particle effect type " + particleEffectType.getName() + ", no particleFile, customImplementation or fragment and vertex shader");
			}
		}
	}

	public ParticleEffectInstance create(ParticleEffectType type, Optional<Entity> parentEntity,
										 Optional<MapTile> parentTile, Optional<Color> optionalMaterialColor) {
		if (type.getCustomImplementation() != null) {
			if (parentEntity.isPresent()) {
				return customEffectFactory.createProgressBarEffect(parentEntity.get());
			} else {
				Logger.error("Custom implementations are currently for entity attached effects only");
				return null;
			}
		} else if (type.getFragmentShaderFile() != null && type.getVertexShaderFile() != null) {
			return buildShaderEffect(type, parentEntity, parentTile);
		} else {
  			return buildLibgdxParticleEffect(type, parentEntity, parentTile, optionalMaterialColor);
		}
	}

	private ParticleEffectInstance buildShaderEffect(ParticleEffectType type, Optional<Entity> parentEntity, Optional<MapTile> parentTile) {
		ShaderEffect shaderEffectBaseInstance = shaderEffectBaseInstances.get(type);
		ShaderEffect clonedInstance = new ShaderEffect(shaderEffectBaseInstance);

		ParticleEffectInstance instance;
		if (parentEntity.isPresent()) {
			instance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), type, clonedInstance, parentEntity.get());
			instance.setPositionToParent();

			EntityAssetOrientation parentOrientation = parentEntity.get().getLocationComponent().getOrientation().toOrthogonal();
			if (type.getUsingParentOrientation() != null) {
				adjustForParentOrientation(instance, parentOrientation);
			}

			Vector2 offset = parentOrientation.toVector2().cpy().scl(type.getDistanceFromParentEntityOrientation());
			instance.setOffsetFromWorldPosition(offset.add(instance.getOffsetFromWorldPosition()));
		} else if (parentTile.isPresent()) {
			instance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), type, clonedInstance, parentTile.get());
			instance.setPositionToParent();
		} else {
			return null;
		}
		return instance;
	}

	private ParticleEffectInstance buildLibgdxParticleEffect(ParticleEffectType type, Optional<Entity> parentEntity,
															 Optional<MapTile> parentTile, Optional<Color> optionalMaterialColor) {
		LibgdxParticleEffect gdxBaseInstance = baseInstancesByDefinition.get(type);
		LibgdxParticleEffect gdxClone = new LibgdxParticleEffect(gdxBaseInstance);

		if (type.isUsesTargetMaterialAsTintColor()) {
			if (optionalMaterialColor.isPresent()) {
				gdxClone.setTint(optionalMaterialColor.get());
			} else {
				// Uses target color but there is no color supplied, so skip this effect
				return null;
			}
		}

		ParticleEffectInstance instance;
		if (parentEntity.isPresent()) {
			instance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), type, gdxClone, parentEntity.get());

			instance.setPositionToParent();

			EntityAssetOrientation parentOrientation = parentEntity.get().getLocationComponent().getOrientation().toOrthogonal();
			if (type.getUsingParentOrientation() != null) {
				adjustForParentOrientation(instance, parentOrientation);
			}

			Vector2 offset = parentOrientation.toVector2().cpy().scl(type.getDistanceFromParentEntityOrientation());
			instance.setOffsetFromWorldPosition(offset);
		} else if (parentTile.isPresent()) {
			instance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), type, gdxClone, parentTile.get());
			instance.setPositionToParent();
		} else {
			return null;
		}

		gdxClone.start();

		return instance;
	}

	private void adjustForParentOrientation(ParticleEffectInstance instance, EntityAssetOrientation parentOrientation) {
		EntityAssetOrientation effectDefaultOrientation = instance.getType().getUsingParentOrientation();

		if (!parentOrientation.equals(effectDefaultOrientation)) {
			instance.getWrappedInstance().adjustForParentOrientation(effectDefaultOrientation, parentOrientation);
		}
	}

}
