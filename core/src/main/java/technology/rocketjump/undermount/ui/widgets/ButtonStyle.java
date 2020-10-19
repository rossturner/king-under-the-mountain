package technology.rocketjump.undermount.ui.widgets;

import static technology.rocketjump.undermount.ui.widgets.IconButton.*;

public enum ButtonStyle {

	HALF_SIZE_NO_TEXT((ICON_PX_WIDTH_HEIGHT / 2) + (2 * MARGIN),
			(ICON_PX_WIDTH_HEIGHT / 2) + (2 * MARGIN), 0),

	SMALL((ICON_PX_WIDTH_HEIGHT * 2) + (2 * MARGIN),
			(ICON_PX_WIDTH_HEIGHT / 2) + (2*MARGIN),
			DEFAULT_TEXT_HEIGHT),

	DEFAULT(ICON_PX_WIDTH_HEIGHT + (2 * MARGIN),
			ICON_PX_WIDTH_HEIGHT + (2*MARGIN) + DEFAULT_TEXT_HEIGHT + TEXT_MARGIN,
			DEFAULT_TEXT_HEIGHT
	),

	LARGE((2 * ICON_PX_WIDTH_HEIGHT) + (2 * MARGIN),
			ICON_PX_WIDTH_HEIGHT + (2*MARGIN) + (2* DEFAULT_TEXT_HEIGHT) + (2*TEXT_MARGIN),
			DEFAULT_TEXT_HEIGHT * 2 + TEXT_MARGIN
	),

	EXTRA_WIDE((4 * ICON_PX_WIDTH_HEIGHT) + (2 * MARGIN),
		ICON_PX_WIDTH_HEIGHT + (2*MARGIN) + (2* DEFAULT_TEXT_HEIGHT) + (2*TEXT_MARGIN),
		DEFAULT_TEXT_HEIGHT * 2 + TEXT_MARGIN
	);

	public final float MAX_WIDTH, MAX_HEIGHT;
	public final float LABEL_HEIGHT;

	ButtonStyle(float maxWidth, float maxHeight, float labelHeight) {
		MAX_WIDTH = maxWidth;
		MAX_HEIGHT = maxHeight;
		LABEL_HEIGHT = labelHeight;
	}

}
