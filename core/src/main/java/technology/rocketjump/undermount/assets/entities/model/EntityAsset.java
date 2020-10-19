package technology.rocketjump.undermount.assets.entities.model;

import java.util.List;
import java.util.Map;

public interface EntityAsset {

	Map<EntityAssetOrientation, SpriteDescriptor> getSpriteDescriptors();

	EntityAssetType getType();

	Map<String, List<String>> getTags();


	String getUniqueName();

	// Passed on from EntityChildAssetDescriptor to adjust render layering
	// e.g. to show an attached hand behind a held item
	Integer getOverrideRenderLayer();
	void setOverrideRenderLayer(Integer overrideRenderLayer);

}
