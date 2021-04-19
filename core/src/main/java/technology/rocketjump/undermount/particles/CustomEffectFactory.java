package technology.rocketjump.undermount.particles;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.particles.custom_libgdx.ParticleEffect;
import technology.rocketjump.undermount.particles.custom_libgdx.ProgressBarEffect;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;

import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS;

@Singleton
public class CustomEffectFactory {

	public static final String PROGRESS_BAR_EFFECT_TYPE_NAME = "Progress bar";
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	private final Sprite progressBarOuterSprite;
	private final Sprite progressBarInnerSprite;
	private final ParticleEffectType progressBarEffectType;

	@Inject
	public CustomEffectFactory(TextureAtlasRepository textureAtlasRepository, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;

		// TODO more generic loading of custom effects
		progressBarEffectType = particleEffectTypeDictionary.getByName(PROGRESS_BAR_EFFECT_TYPE_NAME);

		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(GUI_TEXTURE_ATLAS);

		progressBarOuterSprite = diffuseTextureAtlas.createSprite("yellow_button13");
		progressBarInnerSprite = diffuseTextureAtlas.createSprite("green_button01");
	}


	public ParticleEffectInstance createProgressBarEffect(Entity parentEntity) {
		ParticleEffect progressBarEffect = new ProgressBarEffect(progressBarInnerSprite, progressBarOuterSprite);
		ParticleEffectInstance particleEffectInstance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), progressBarEffectType, progressBarEffect, parentEntity);
		particleEffectInstance.setOffsetFromWorldPosition(progressBarEffectType.getOffsetFromParentEntity());

		return particleEffectInstance;
	}
}
