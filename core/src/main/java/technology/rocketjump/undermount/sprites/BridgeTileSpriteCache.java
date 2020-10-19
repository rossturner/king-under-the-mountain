package technology.rocketjump.undermount.sprites;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.sprites.model.BridgeOrientation;
import technology.rocketjump.undermount.sprites.model.BridgeTileLayout;
import technology.rocketjump.undermount.sprites.model.BridgeType;

import java.util.EnumMap;
import java.util.Map;

@Singleton
public class BridgeTileSpriteCache {

	private Map<GameMaterialType, Map<BridgeOrientation, Map<BridgeTileLayout, Sprite>>> materialMap = new EnumMap<>(GameMaterialType.class);

	@Inject
	public BridgeTileSpriteCache(TextureAtlas textureAtlas, BridgeTypeDictionary bridgeTypeDictionary) {
		for (BridgeType bridgeType : bridgeTypeDictionary.getAll()) {
			for (Map.Entry<BridgeOrientation, Map<BridgeTileLayout, String>> orientationEntry : bridgeType.getAssets().entrySet()) {
				for (Map.Entry<BridgeTileLayout, String> layoutEntry : orientationEntry.getValue().entrySet()) {
					Sprite sprite = textureAtlas.createSprite(layoutEntry.getValue().substring(0, layoutEntry.getValue().lastIndexOf('.')));
					if (sprite == null) {
						throw new RuntimeException("Could not find bridge sprite with name " + layoutEntry.getValue() + " in texture atlas " + textureAtlas);
					} else {
						Map<BridgeOrientation, Map<BridgeTileLayout, Sprite>> orientationMap = materialMap.computeIfAbsent(bridgeType.getMaterialType(), (x) -> new EnumMap<>(BridgeOrientation.class));
						Map<BridgeTileLayout, Sprite> layoutMap = orientationMap.computeIfAbsent(orientationEntry.getKey(), (a) -> new EnumMap<>(BridgeTileLayout.class));
						layoutMap.put(layoutEntry.getKey(), sprite);
					}
				}
			}
		}
	}


	public Sprite getForBridge(GameMaterialType materialType, BridgeOrientation bridgeOrientation, BridgeTileLayout layout) {
		Map<BridgeOrientation, Map<BridgeTileLayout, Sprite>> orientationMap = materialMap.get(materialType);
		if (orientationMap == null) {
			Logger.error("No bridges of type " + materialType + " which was requested");
		} else {
			Map<BridgeTileLayout, Sprite> layoutMap = orientationMap.get(bridgeOrientation);
			if (layoutMap == null) {
				Logger.error("No sprites by orientation " + bridgeOrientation + " for " + materialType);
			} else {
				Sprite sprite = layoutMap.get(layout);
				if (sprite == null) {
					Logger.error("No sprite by layout " + layout + " for bridge " + bridgeOrientation + " " + materialType);
				} else {
					return sprite;
				}
			}
		}
		return null;
	}

}
