package technology.rocketjump.undermount.assets.entities.humanoid.model;

import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.assets.entities.model.SpriteDescriptor;

import java.util.EnumMap;
import java.util.Map;

/**
 * This class represents a group of sprites in different orientations that represent a single asset e.g. a dwarf body
 */
public class HumanoidEntityAsset extends HumanoidEntityAssetDescriptor implements EntityAsset {

	private Map<EntityAssetOrientation, SpriteDescriptor> spriteDescriptors = new EnumMap<>(EntityAssetOrientation.class);

	@Override
	public Map<EntityAssetOrientation, SpriteDescriptor> getSpriteDescriptors() {
		return spriteDescriptors;
	}

	private Integer overrideRenderLayer;

	@Override
	public Integer getOverrideRenderLayer() {
		return overrideRenderLayer;
	}

	@Override
	public void setOverrideRenderLayer(Integer overrideRenderLayer) {
		this.overrideRenderLayer = overrideRenderLayer;
	}
}
