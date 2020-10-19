package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.Screen;
import technology.rocketjump.undermount.ui.widgets.GameDialog;

public interface GameScreen extends Screen {

	String getName();

	void showDialog(GameDialog dialog);

}
