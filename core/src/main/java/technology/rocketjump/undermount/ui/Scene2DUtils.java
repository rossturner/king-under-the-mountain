package technology.rocketjump.undermount.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class Scene2DUtils {

	public static EventListener setGainScrollOnHover(final Actor scrollPane) {
		return event -> {
			if(event instanceof InputEvent)
				if(((InputEvent)event).getType() == InputEvent.Type.enter)
					event.getStage().setScrollFocus(scrollPane);
			return false;
		};
	}

	public static EventListener setLoseScrollOnHoverExit() {
		return event -> {
			if(event instanceof InputEvent)
				if(((InputEvent)event).getType() == InputEvent.Type.exit)
					event.getStage().setScrollFocus(null);
			return false;
		};
	}

	public static ScrollPane wrapWithScrollPane(Table tableToMakeScrollable, Skin uiSkin) {
		ScrollPane scrollPane = new ScrollPane(tableToMakeScrollable, uiSkin);
		scrollPane.addListener(Scene2DUtils.setGainScrollOnHover(scrollPane));
		scrollPane.addListener(Scene2DUtils.setLoseScrollOnHoverExit());

		scrollPane.setScrollingDisabled(true, false);
		scrollPane.setForceScroll(false, true);
		ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle(uiSkin.get(ScrollPane.ScrollPaneStyle.class));
		scrollPaneStyle.background = null;
		scrollPane.setStyle(scrollPaneStyle);
		scrollPane.setFadeScrollBars(false);
		return scrollPane;
	}

}
