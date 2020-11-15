package technology.rocketjump.undermount.jobs.model;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public enum JobPriority {

	// These are expected to be in order of highest to lowest priority
	HIGHEST("#e4492b", "fast-forward-button-up", "PRIORITY.HIGHEST"),
	HIGHER("#d2942f", "play-button-up", "PRIORITY.HIGHER"),
	NORMAL("#44aa26", "play-button", "PRIORITY.NORMAL"),
	LOWER("#20b1bd", "play-button-down", "PRIORITY.LOWER"),
	LOWEST("#243aba", "fast-forward-button-down", "PRIORITY.LOWEST");

	public final Color color;
	public final Color semiTransparentColor;
	public final String iconName;
	public final String i18nKey;

	JobPriority(String hexColor, String iconName, String i18nKey) {
		this.color = HexColors.get(hexColor);
		this.semiTransparentColor = this.color.cpy();
		this.semiTransparentColor.a = 0.8f;
		this.iconName = iconName;
		this.i18nKey = i18nKey;
	}

}
