package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.scenes.scene2d.Actor;

public class TooltipMessage {
	public final String tooltipI18nKey;
	public final Actor parent;

	public TooltipMessage(String tooltipI18nKey, Actor parent) {
		this.tooltipI18nKey = tooltipI18nKey;
		this.parent = parent;
	}
}
