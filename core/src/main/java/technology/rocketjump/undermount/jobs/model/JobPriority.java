package technology.rocketjump.undermount.jobs.model;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public enum JobPriority {

	// These are expected to be in order of highest to lowest priority
	HIGHEST("#265aaa", "fast-forward-button-up", "PRIORITY.HIGHEST"),
	HIGHER("#269caa", "play-button-up", "PRIORITY.HIGHER"),
	NORMAL("#27aa5e", "play-button", "PRIORITY.NORMAL"),
	LOWER("#bab524", "play-button-down", "PRIORITY.LOWER"),
	LOWEST("#aa8026", "fast-forward-button-down", "PRIORITY.LOWEST"),
	DISABLED("#D4534C", "cancel", "PRIORITY.DISABLED");

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
