package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

public interface GuiView {

	void populate(Table containerTable);

	void update();

	GuiViewName getName();

	GuiViewName getParentViewName();

}
