package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;

/**
 * This class draws text on top of the screen,
 * mostly used for debugging purposes, but also renders dragged area size as WidthxHeight near cursor
 */
@Singleton
public class ScreenWriter {

	private static final float LINE_HEIGHT = 20f;
	private final Viewport viewport;
	private final Label label;
	private final Label dragSizeLabel;
	private Stage stage;
	private Array<String> lines = new Array<>();
	public Vector2 offsetPosition = new Vector2();
	private boolean dragging;


	@Inject
	public ScreenWriter(GuiSkinRepository guiSkinRepository) {
		viewport = new ScreenViewport(); // Default viewport sets up the screen in window pixel size, 0, 0 bottom-left
		stage = new Stage(viewport);

		label = new Label("Default text", guiSkinRepository.getDefault());
		label.setPosition(20f, Gdx.graphics.getHeight() -  30f);

		dragSizeLabel = new Label("Test", guiSkinRepository.getDefault());

		stage.addActor(label);
	}

	public void clearText() {
		lines.clear();
	}

	public void printLine(String line) {
		lines.add(line);
	}

	public void render() {
		StringBuilder linesBuilder = new StringBuilder();
		for (String line : lines) {
			linesBuilder.append(line).append("\n");
		}
		label.setText(linesBuilder.toString());
		Vector2 basePosition = new Vector2(20f, Gdx.graphics.getHeight() - 20f - (lines.size * LINE_HEIGHT));
		basePosition.add(offsetPosition);
		label.setPosition(basePosition.x, basePosition.y);

		if (dragging) {
			if (!dragSizeLabel.hasParent()) {
				stage.addActor(dragSizeLabel);
			}
			dragSizeLabel.setPosition(Gdx.input.getX() + 5, Gdx.graphics.getHeight() - Gdx.input.getY() + 5);
		} else {
			dragSizeLabel.remove();
		}

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	public void onResize(int screenWidth, int screenHeight) {
		viewport.update(screenWidth, screenHeight, true);
	}

	private int currentTileWidth = 0;
	private int currentTileHeight = 0;

	public int getCurrentTileWidth() {
		return currentTileWidth;
	}

	public int getCurrentTileHeight() {
		return currentTileHeight;
	}

	public void setDragging(boolean isDragging, int width, int height) {
		if (currentTileWidth != width || currentTileHeight != height) {
			currentTileWidth = width;
			currentTileHeight = height;
			dragSizeLabel.setText(width + "x" + height);
		}
		this.dragging = isDragging;
	}
}
