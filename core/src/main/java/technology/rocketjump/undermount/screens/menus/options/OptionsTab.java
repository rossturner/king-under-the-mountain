package technology.rocketjump.undermount.screens.menus.options;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

public interface OptionsTab {

	void populate(Table menuTable);

	OptionsTabName getTabName();

}
