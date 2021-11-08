package technology.rocketjump.undermount.assets.entities;

import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.undermount.assets.entities.model.EntityAssetType.UNSPECIFIED;
import static technology.rocketjump.undermount.entities.model.EntityType.CREATURE;

public class RenderLayerDictionaryTest {

	private RenderLayerDictionary renderLayerDictionary;
	private EntityAssetTypeDictionary entityAssetTypeDictionary;

	@Before
	public void setup() throws IOException {
		this.entityAssetTypeDictionary = new EntityAssetTypeDictionary(new File("assets/definitions/entityAssets/entityAssetTypes.json"));
		renderLayerDictionary = new RenderLayerDictionary(new File("assets/definitions/entityAssets/renderLayers.json"), entityAssetTypeDictionary);
	}

	@Test
	public void testGetRenderingLayer_withHeldValues() {
		assertThat(renderLayerDictionary.getRenderingLayer(CREATURE, DOWN, entityAssetTypeDictionary.getByName("CREATURE_BODY"))).isEqualTo(0);
		assertThat(renderLayerDictionary.getRenderingLayer(CREATURE, DOWN, entityAssetTypeDictionary.getByName("BODY_CLOTHING"))).isEqualTo(1);
		assertThat(renderLayerDictionary.getRenderingLayer(CREATURE, DOWN, entityAssetTypeDictionary.getByName("BODY_OUTLINE"))).isEqualTo(2);
	}

	@Test
	public void testGetRenderingLayer_withUnknownValues() {
		// Probably have to remove the below line when all types have assets
		assertThat(renderLayerDictionary.getRenderingLayer(CREATURE, null, entityAssetTypeDictionary.getByName("CREATURE_BODY"))).isEqualTo(-2);
		EntityAssetType unspecifiedAsset = new EntityAssetType(UNSPECIFIED);
		assertThat(renderLayerDictionary.getRenderingLayer(CREATURE, DOWN, unspecifiedAsset)).isEqualTo(100);
	}

}