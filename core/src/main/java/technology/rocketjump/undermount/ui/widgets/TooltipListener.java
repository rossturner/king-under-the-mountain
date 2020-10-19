package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.StringBuilder;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.TooltipMessage;

public class TooltipListener extends ClickListener {

	private final String tooltipI18nKey;
	private final MessageDispatcher messageDispatcher;
	private final Actor parent;

	public TooltipListener(Actor parent, String tooltipI18nKey, MessageDispatcher messageDispatcher) {
		if (!(parent instanceof Label) && !(parent instanceof IconButton)) {
			throw new RuntimeException(this.getClass().getSimpleName() + " is only supported on Labels");
		}
		this.parent = parent;
		this.tooltipI18nKey = tooltipI18nKey;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
		StringBuilder parentText = getText(parent);
		StringBuilder fromActorText = getText(fromActor);
		// Ignore when entering from self e.g. when stage changes
		if (!parentText.equals(fromActorText)) {
			messageDispatcher.dispatchMessage(MessageType.TOOLTIP_AREA_ENTERED,
					new TooltipMessage(tooltipI18nKey, parent));
		}
	}

	@Override
	public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
		StringBuilder parentText = getText(parent);
		StringBuilder toActorText = getText(toActor);
		// Ignore when exiting to same actor i.e. when stage changes
		if (!parentText.equals(toActorText)) {
			messageDispatcher.dispatchMessage(MessageType.TOOLTIP_AREA_EXITED,
					new TooltipMessage(tooltipI18nKey, parent));
		}
	}

	private StringBuilder getText(Actor actor) {
		if (actor == null) {
			return null;
		} else if (actor instanceof Label) {
			return ((Label)actor).getText();
		} else if (actor instanceof IconButton) {
			return ((IconButton)actor).getText();
		} else {
			return null;
		}
	}
}
