package technology.rocketjump.undermount.screens.menus;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

public interface Menu {

	void show();

	void hide();

	void populate(Table containerTable);

	void reset();

}
