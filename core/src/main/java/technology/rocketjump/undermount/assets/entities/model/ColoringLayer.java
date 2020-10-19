package technology.rocketjump.undermount.assets.entities.model;

import technology.rocketjump.undermount.materials.model.GameMaterialType;

import static technology.rocketjump.undermount.materials.model.GameMaterialType.*;

/**
 * Defines the colors used for coloring-in special layers of an asset
 */
public enum ColoringLayer {

	// Humanoid layers
	HAIR_COLOR, SKIN_COLOR, EYE_COLOR, ACCESSORY_COLOR, BONE_COLOR,

	// Plant type layers
	BRANCHES_COLOR, LEAF_COLOR, FRUIT_COLOR, FLOWER_COLOR,

	// Misc colors to enable different colors of same material
	MISC_COLOR_1, MISC_COLOR_2, MISC_COLOR_3, MISC_COLOR_4, MISC_COLOR_5, MISC_COLOR_6, MISC_COLOR_7,

	// From GameMaterialType
	SEED_COLOR(SEED), VEGETABLE_COLOR(VEGETABLE),
	CLOTH_COLOR(CLOTH), ROPE_COLOR(ROPE), EARTH_COLOR(EARTH),
	STONE_COLOR(STONE), ORE_COLOR(ORE), GEM_COLOR(GEM), METAL_COLOR(METAL), WOOD_COLOR(WOOD),
	VITRIOL_COLOR(VITRIOL),
	FOODSTUFF_COLOR(FOODSTUFF), LIQUID_COLOR(LIQUID), OTHER_COLOR(OTHER);

	private final GameMaterialType linkedMaterialType;

	ColoringLayer() {
		linkedMaterialType = null;
	}

	ColoringLayer(GameMaterialType linkedMaterialType) {
		this.linkedMaterialType = linkedMaterialType;
	}

	public GameMaterialType getLinkedMaterialType() {
		return linkedMaterialType;
	}

	public static ColoringLayer getByMaterialType(GameMaterialType materialType) {
		for (ColoringLayer coloringLayer : ColoringLayer.values()) {
			if (materialType.equals(coloringLayer.getLinkedMaterialType())) {
				return coloringLayer;
			}
		}
		return null;
	}
}
