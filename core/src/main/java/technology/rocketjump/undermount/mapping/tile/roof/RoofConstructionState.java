package technology.rocketjump.undermount.mapping.tile.roof;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public enum RoofConstructionState {

	NONE(Color.CLEAR),
	PENDING(HexColors.get("#FFFF9966")),
	TOO_FAR_FROM_SUPPORT(HexColors.get("#ff742166")),
	NO_ADJACENT_ROOF(HexColors.get("#d2d14466")),
	READY_FOR_CONSTRUCTION(HexColors.get("#FFFFFF66")),
	PENDING_DECONSTRUCTION(HexColors.get("#EE332E66"));

	public final Color renderColor;

	RoofConstructionState(Color renderColor) {
		this.renderColor = renderColor;
	}
}
