package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.ui.actions.ButtonAction;

public class IconOnlyButton extends Table {

	public final GameSpeed gameSpeed;
	private Image currentIcon;
	private Color defaultBackgroundColor = Color.WHITE;
	private Color defaultForegroundColor = Color.GRAY;
	private Color currentBackgroundColor = defaultBackgroundColor;
	private Color currentForegroundColor = defaultForegroundColor;
	private ButtonAction action;

	private float scale = 0.5f;

	public IconOnlyButton(GameSpeed gameSpeed) {
		this.gameSpeed = gameSpeed;
		setTouchable(Touchable.enabled);
		addListener(new IconOnlyButtonClickListener(this));
	}

	private void resetChildren() {
		this.clearChildren();
		if (currentIcon != null) {
			Cell<Image> cell = this.add(currentIcon).pad(0);
			cell.height(currentIcon.getHeight()).width(currentIcon.getWidth());
		}
	}

	public void setIconSprite(Sprite iconSprite) {
		this.currentIcon = new Image(iconSprite);
		currentIcon.setSize(currentIcon.getWidth() * scale, currentIcon.getHeight() * scale);
		resetChildren();
	}

	public void setHighlighted(boolean highlight) {
		if (highlight) {
			currentForegroundColor = defaultBackgroundColor;
			currentBackgroundColor = defaultForegroundColor;
		} else {
			currentForegroundColor = defaultForegroundColor;
			currentBackgroundColor = defaultBackgroundColor;
		}
	}

	@Override
	public void act(float delta) {
		if (currentIcon != null) {
			this.currentIcon.setColor(currentForegroundColor);
		}
	}

	@Override
	protected void drawBackground (Batch batch, float parentAlpha, float x, float y) {
		if (this.getBackground() == null) return;
		Color color = currentBackgroundColor;
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		getBackground().draw(batch, x, y, getWidth(), getHeight());
	}

	public void setAction(ButtonAction action) {
		this.action = action;
	}

	protected static class IconOnlyButtonClickListener extends ClickListener {

		protected final IconOnlyButton parent;

		public IconOnlyButtonClickListener(IconOnlyButton parent) {
			this.parent = parent;
		}

		public void clicked (InputEvent event, float x, float y) {
			if (parent.action == null) {
				Logger.error("No action set for " + IconOnlyButton.class.getSimpleName());
			} else {
				parent.action.onClick();
			}
		}

	}

}
