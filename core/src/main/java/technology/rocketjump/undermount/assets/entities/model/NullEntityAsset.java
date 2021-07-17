package technology.rocketjump.undermount.assets.entities.model;

import java.util.List;
import java.util.Map;

public class NullEntityAsset implements EntityAsset {

	public static final EntityAsset NULL_ASSET = new NullEntityAsset();

	@Override
	public Map<EntityAssetOrientation, SpriteDescriptor> getSpriteDescriptors() {
		return null;
	}

	@Override
	public EntityAssetType getType() {
		return null;
	}

	@Override
	public Map<String, List<String>> getTags() {
		return null;
	}

	@Override
	public String getUniqueName() {
		return "NULL_ASSET";
	}

	@Override
	public Integer getOverrideRenderLayer() {
		return null;
	}

	@Override
	public void setOverrideRenderLayer(Integer overrideRenderLayer) {

	}
}
