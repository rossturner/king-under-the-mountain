package technology.rocketjump.undermount.rendering.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.undermount.assets.entities.RenderLayerDictionary;
import technology.rocketjump.undermount.assets.entities.model.*;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.AttachedEntity;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.rendering.RenderMode;
import technology.rocketjump.undermount.rendering.custom_libgdx.ShaderLoader;

import java.util.Map;

import static technology.rocketjump.undermount.assets.entities.model.NullEntityAsset.NULL_ASSET;
import static technology.rocketjump.undermount.entities.model.EntityType.STATIC_ENTITY_TYPES;
import static technology.rocketjump.undermount.mapping.tile.roof.TileRoofState.OPEN;

@Singleton
public class EntityRenderer implements GameContextAware, Disposable {

	public static float PIXELS_PER_TILE = 64f;

	private EntityRenderSteps entityRenderSteps = new EntityRenderSteps();
	private final CompleteAssetDictionary assetDictionary;
	private final RenderLayerDictionary renderLayerDictionary;

	private boolean usingNormalMapInverseShader = false;
	private ShaderProgram defaultShader;
	private ShaderProgram inverseNormalShader;
	private ShaderProgram snowShader;
	private GameContext gameContext;

	@Inject
	public EntityRenderer(CompleteAssetDictionary completeAssetDictionary,
						  RenderLayerDictionary renderLayerDictionary) {
		this.assetDictionary = completeAssetDictionary;
		this.renderLayerDictionary = renderLayerDictionary;

		FileHandle defaultVertexShaderFile = Gdx.files.classpath("shaders/default_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/invert_normal_map_red_channel_fragment_shader.glsl");
		inverseNormalShader = ShaderLoader.createShader(defaultVertexShaderFile, fragmentShaderFile);

		FileHandle snowFragmentShaderFile = Gdx.files.classpath("shaders/snow_fragment_shader.glsl");
		snowShader = ShaderLoader.createShader(defaultVertexShaderFile, snowFragmentShaderFile);
	}

	/**
	 * Renders the given entity at its world position (so it assumes the projection matrix has been set up already)
	 *
	 * @param entity           The entity, should contain all necessary information
	 * @param basicSpriteBatch
	 * @param renderMode
	 * @param parentEntity     Used when this is a held item and parent entity hands need attaching
	 * @param overrideColor
	 * @param extraMultiplyColor
	 * @params renderNormals draw normal map texture rather than diffuse
	 */
	public void render(Entity entity, Batch basicSpriteBatch, RenderMode renderMode, Entity parentEntity, Color overrideColor, Color extraMultiplyColor) {
		if (renderMode.equals(RenderMode.NORMALS) && overrideColor != null) {
			return; // Don't render normals for entities with override color e.g. construction "ghosts"
		}

		if (defaultShader == null) {
			defaultShader = basicSpriteBatch.getShader();
		}

		entityRenderSteps.clear();

		LocationComponent locationComponent = entity.getLocationComponent();

		EntityAsset baseAsset = entity.getPhysicalEntityComponent().getBaseAsset();
		if (baseAsset != NULL_ASSET) {
			addToRenderParts(baseAsset, entity, new Vector2(), assetDictionary, parentEntity);
		}

		addAttachedEntitiesAsRenderSteps(entity);

		for (EntityPartRenderStep renderStep : entityRenderSteps.clone().getRenderSteps()) {
			render(renderStep, basicSpriteBatch, locationComponent, renderMode, overrideColor, extraMultiplyColor);
		}

		if (renderMode.equals(RenderMode.NORMALS) && usingNormalMapInverseShader) {
			usingNormalMapInverseShader = false;
			basicSpriteBatch.setShader(defaultShader);
		}
	}

	private void addAttachedEntitiesAsRenderSteps(Entity entity) {
		for (AttachedEntity attachedEntity : entity.getAttachedEntities()) {
			entityRenderSteps.addAttachedEntity(attachedEntity);
		}

		for (Map.Entry<EntityAssetType, Entity> attached : entityRenderSteps.getAttachedEntities()) {
			AttachmentDescriptor attachment = entityRenderSteps.getAttachmentPoint(attached.getKey());
			if (attachment != null) {
				Vector2 attachmentOffsetPosition = attachment.getOffsetPosition();

				EntityPartRenderStep attachedRenderStep = new EntityPartRenderStep(null, attachmentOffsetPosition, entity);
				attachedRenderStep.setOtherEntity(attached.getValue());

				int renderLayer = renderLayerDictionary.getRenderingLayer(entity.getType(),
						entity.getLocationComponent().getOrientation(), attached.getKey());
				if (attachment.getOverrideRenderLayer() != null) {
					renderLayer += attachment.getOverrideRenderLayer();
				}

				entityRenderSteps.addPartToRender(renderLayer, attachedRenderStep);
			}
		}
	}


	private void addToRenderParts(EntityAsset asset, Entity entity, Vector2 parentPosition, CompleteAssetDictionary assetDictionary, Entity parentEntity) {
		if (asset == null) {
			Logger.error("Attempting to render null asset for " + entity);
			return;
		}
		SpriteDescriptor spriteDescriptor = asset.getSpriteDescriptors().get(entity.getLocationComponent().getOrientation());
		if (spriteDescriptor == null) {
			// FIXME no sprite descriptor when one was expected
			return;
		}
		int renderLayer = renderLayerDictionary.getRenderingLayer(entity.getType(),
				entity.getLocationComponent().getOrientation(), asset.getType());
		if (asset.getOverrideRenderLayer() != null) {
			renderLayer += asset.getOverrideRenderLayer();
		}

		entityRenderSteps.addPartToRender(renderLayer, new EntityPartRenderStep(spriteDescriptor, parentPosition, entity));

		for (EntityChildAssetDescriptor childAssetDescriptor : spriteDescriptor.getChildAssets()) {
			EntityAsset childAsset = getAssetByDescriptor(entity, childAssetDescriptor, assetDictionary);
			// FIXME This is duplicated a little with the code below for parentEntityAssets
			if (childAsset != null && !childAsset.getSpriteDescriptors().isEmpty()) {
				Vector2 childOffset = childAssetDescriptor.getOffsetPixels().cpy().scl(spriteDescriptor.getScale() * (1f / PIXELS_PER_TILE));
				addToRenderParts(childAsset, entity, parentPosition.cpy().add(childOffset), assetDictionary, parentEntity);
			}
		}

		if (!spriteDescriptor.getAttachmentPoints().isEmpty()) {
			entityRenderSteps.addAttachmentPoints(spriteDescriptor.getAttachmentPoints(), parentPosition.cpy());
		}

		// FIXME If we could specify per-orientation if the attached parent entity asset was to go above or below, it would look better
		if (!spriteDescriptor.getParentEntityAssets().isEmpty() && parentEntity != null) {
			for (EntityChildAssetDescriptor descriptorForParentAsset : spriteDescriptor.getParentEntityAssets()) {
				EntityAsset parentEntityAsset = getAssetByDescriptor(parentEntity, descriptorForParentAsset, assetDictionary);
				if (parentEntityAsset != null && !parentEntityAsset.getSpriteDescriptors().isEmpty()) {
					Vector2 offsetWorldPosition = descriptorForParentAsset.getOffsetPixels().cpy().scl(spriteDescriptor.getScale() * (1f / PIXELS_PER_TILE));
					addToRenderParts(parentEntityAsset, parentEntity, parentPosition.cpy().add(offsetWorldPosition), assetDictionary, null);
				}
			}
		}
	}

	private EntityAsset getAssetByDescriptor(Entity entity, EntityChildAssetDescriptor assetDescriptor, CompleteAssetDictionary assetDictionary) {
		EntityAsset asset;
		if (assetDescriptor.getSpecificAssetName() != null) {
			// FIXME https://github.com/rossturner/king-under-the-mountain/issues/18
			// Specific assets should be found at setup time
			asset = assetDictionary.getByUniqueName(assetDescriptor.getSpecificAssetName());
		} else {
			asset = entity.getPhysicalEntityComponent().getTypeMap().get(assetDescriptor.getType());
		}
		if (asset != null && assetDescriptor.getOverrideRenderLayer() != null) {
			asset.setOverrideRenderLayer(assetDescriptor.getOverrideRenderLayer());
		}
		return asset;
	}

	private void render(EntityPartRenderStep renderStep, Batch spriteBatch, LocationComponent locationComponent, RenderMode renderMode,
						Color overrideColor, Color extraMultiplyColor) {
		if (renderStep.isAnotherEntity()) {
			Entity entity = renderStep.getEntity();
			LocationComponent otherEntityLocation = renderStep.getOtherEntity().getLocationComponent();
			Vector2 worldPosition = entity.getLocationComponent().getWorldPosition();
			Vector2 offset = renderStep.getOffsetFromEntity();
			float originalRotation = otherEntityLocation.getRotation();
			Vector2 otherEntityLocationOriginalPosition = otherEntityLocation.getWorldPosition();
			if (entity.getLocationComponent().getRotation() != 0) {
				offset.cpy().rotate(entity.getLocationComponent().getRotation());
				otherEntityLocation.setRotation(originalRotation + entity.getLocationComponent().getRotation());
			}
			otherEntityLocation.setWorldPosition(worldPosition.cpy().add(offset), false, false);
			EntityRenderSteps cloned = entityRenderSteps.clone();
			this.render(renderStep.getOtherEntity(), spriteBatch, renderMode, renderStep.getEntity(), overrideColor, extraMultiplyColor);
			entityRenderSteps = cloned;
			otherEntityLocation.setWorldPosition(otherEntityLocationOriginalPosition, false, false);
			otherEntityLocation.setRotation(originalRotation);
			return;
		}

		if (overrideColor == null) {
			spriteBatch.setColor(Color.WHITE);
		} else {
			spriteBatch.setColor(overrideColor);
		}
		if (extraMultiplyColor != null) {
			spriteBatch.setColor(spriteBatch.getColor().cpy().mul(extraMultiplyColor));
		}


		SpriteDescriptor spriteDescriptor = renderStep.getSpriteDescriptor();

		EntityAttributes attributes = renderStep.getEntity().getPhysicalEntityComponent().getAttributes();
		if (renderMode.equals(RenderMode.DIFFUSE)) {
			if (spriteDescriptor.getColoringLayer() != null && overrideColor == null) { // Ignore coloring layers when override is set
				Color color = attributes.getColor(spriteDescriptor.getColoringLayer());
				if (color != null) {
					spriteBatch.setColor(color);
					if (extraMultiplyColor != null) {
						spriteBatch.setColor(spriteBatch.getColor().cpy().mul(extraMultiplyColor));
					}
				}
			}
			if (usingNormalMapInverseShader) {
				usingNormalMapInverseShader = false;
				spriteBatch.setShader(defaultShader);
			}
		} else if (renderMode.equals(RenderMode.NORMALS)) {
			if (spriteDescriptor.isFlipX() && !usingNormalMapInverseShader) {
				usingNormalMapInverseShader = true;
				spriteBatch.setShader(inverseNormalShader);
			} else if (!spriteDescriptor.isFlipX() && usingNormalMapInverseShader) {
				usingNormalMapInverseShader = false;
				spriteBatch.setShader(defaultShader);
			}
		}

		float spriteScale = spriteDescriptor.getScale();
		if (attributes instanceof PlantEntityAttributes) {
			PlantEntityAttributes plantEntityAttributes = (PlantEntityAttributes) attributes;
			PlantSpeciesGrowthStage growthStage = plantEntityAttributes.getSpecies().getGrowthStages().get(plantEntityAttributes.getGrowthStageCursor());

			if (growthStage.getInitialPlantScale() < 1) {
				float plantScale = growthStage.getInitialPlantScale() +
						((growthStage.getCompletionPlantScale() - growthStage.getInitialPlantScale()) * plantEntityAttributes.getGrowthStageProgress());
				spriteScale *= plantScale;
			}

			if (ColoringLayer.FRUIT_COLOR.equals(spriteDescriptor.getColoringLayer())) {
				if (growthStage.getInitialFruitScale() < 1) {
					float fruitScale = growthStage.getInitialFruitScale() +
							((growthStage.getCompletionFruitScale() - growthStage.getInitialFruitScale()) * plantEntityAttributes.getGrowthStageProgress());
					spriteScale *= fruitScale;
				}
			}
		}


		Sprite sprite = getSprite(renderStep, renderMode, spriteDescriptor);

		if (sprite == null) {
			if (renderMode.equals(RenderMode.NORMALS)) {
				// Allowing NORMALS render mode to not have sprites at this time FIXME
				return;
			} else {
				Logger.error("Null sprite from descriptor with filename " + spriteDescriptor.getFilename() + " in " + this.getClass().getName());
				return;
			}
		}
		spriteWorldSize.set(
				(sprite.getWidth() * spriteScale) / PIXELS_PER_TILE,
				(sprite.getHeight() * spriteScale) / PIXELS_PER_TILE
		);
		affine.idt(); // Reset affine transformation
		if (locationComponent.getRotation() == 0) {
			affine.translate(locationComponent.getWorldPosition())
					.translate(renderStep.getOffsetFromEntity());
		} else {
			Vector2 offsetFromEntity = renderStep.getOffsetFromEntity().cpy().rotate(locationComponent.getRotation());
			affine.translate(locationComponent.getWorldPosition())
					.translate(offsetFromEntity)
					.rotate(locationComponent.getRotation());
		}
		affine.translate(-spriteWorldSize.x / 2, -spriteWorldSize.y / 2);
		spriteBatch.draw(sprite, spriteWorldSize.x, spriteWorldSize.y, affine);

		if (renderMode.equals(RenderMode.DIFFUSE) && snowRenderingEnabled() && isStaticEntityAndOutside(renderStep.getEntity())) {
			Sprite normalSprite = getSprite(renderStep, RenderMode.NORMALS, spriteDescriptor);
			if (normalSprite != null) {
				ShaderProgram currentShader = spriteBatch.getShader();
				spriteBatch.setShader(snowShader);
				snowShader.setUniformf("u_snowAmount", (float)gameContext.getMapEnvironment().getFallenSnow());
				spriteBatch.setColor(Color.WHITE);
				spriteBatch.draw(normalSprite, spriteWorldSize.x, spriteWorldSize.y, affine);
				spriteBatch.setShader(currentShader);
			}
		}
	}

	private Sprite getSprite(EntityPartRenderStep renderStep, RenderMode renderMode, SpriteDescriptor spriteDescriptor) {
		Sprite sprite = null;

		if (spriteDescriptor.getIsAnimated()) {
			Array<Sprite> animatedSprites = spriteDescriptor.getAnimatedSprites(renderMode);
			if (animatedSprites != null && animatedSprites.size > 0) {
				float animationProgress = renderStep.getEntity().getPhysicalEntityComponent().getAnimationProgress();

				int frameSelection = (int) Math.floor(animationProgress * animatedSprites.size);
				if (frameSelection >= animatedSprites.size) {
					frameSelection = animatedSprites.size - 1;
				} else if (frameSelection <= 0) {
					frameSelection = 0;
				}
				sprite = animatedSprites.get(frameSelection);
			}
		} else {
			sprite = spriteDescriptor.getSprite(renderMode);
		}
		return sprite;
	}

	private Vector2 spriteWorldSize = new Vector2(); // Private member to avoid new instance on every render call

	private boolean snowRenderingEnabled() {
		return gameContext != null && gameContext.getMapEnvironment().getFallenSnow() > 0;
	}

	private Affine2 affine = new Affine2(); // Private member to avoid new instance on every render call
	@Override
	public void dispose() {
		inverseNormalShader.dispose();
	}

	private boolean isStaticEntityAndOutside(Entity entity) {
		if (STATIC_ENTITY_TYPES.contains(entity.getType()) && (entity.getLocationComponent().getContainerEntity() == null)) {
			MapTile mapTile = gameContext.getAreaMap().getTile(entity.getLocationComponent().getWorldPosition());
			return mapTile != null && mapTile.getRoof().getState().equals(OPEN);
		} else {
			return false;
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
