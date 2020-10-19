package technology.rocketjump.undermount.assets.entities.wallcap.model;

import technology.rocketjump.undermount.assets.entities.model.EntityAsset;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.assets.entities.model.SpriteDescriptor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class WallCapAsset extends WallCapAssetDescriptor implements EntityAsset {

	private Map<EntityAssetOrientation, SpriteDescriptor> spriteDescriptors = new EnumMap<>(EntityAssetOrientation.class);

	@Override
	public Map<EntityAssetOrientation, SpriteDescriptor> getSpriteDescriptors() {
		return spriteDescriptors;
	}

	@Override
	public EntityAssetType getType() {
		return type;
	}

	@Override
	public Map<String, List<String>> getTags() {
		return null; // Do wall caps need tags?
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
