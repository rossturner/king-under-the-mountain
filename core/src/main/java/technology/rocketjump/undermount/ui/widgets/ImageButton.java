package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.actions.ButtonAction;

// TODO extended button/table functionality from ImageButton/IconButton/ClickableTable needs refactoring together
public class ImageButton extends Table {

	public static final float HIGHLIGHT_CHANGE_SPEED = 2f; // 1 / How many seconds it takes to change color
	private final boolean smallSize; // TODO Expand this to some style enum/class

	private Sprite iconSprite;
	private Drawable drawable;
	private NinePatch ninePatch;
	private Image currentIcon;

	private Color defaultUnhighlightedColor = HexColors.get("#545E61");
	private Color defaultHighlightedColor = HexColors.get("#f1f3f3");
	private Color currentBackgroundColor = new Color();
	private ButtonAction action;
	private boolean isHighlighted;
	private boolean isTogglable = false;
	private boolean toggledOn = false;
	private float highlight = 0;

	public ImageButton(Sprite iconSprite, NinePatch backgroundNinePatch, boolean smallSize) {
		this.iconSprite = iconSprite;
		this.ninePatch = backgroundNinePatch;

		NinePatchDrawable buttonNinePatchDrawable = new NinePatchDrawable(backgroundNinePatch);
		setBackground(buttonNinePatchDrawable);

		this.currentIcon = new Image(iconSprite);
		this.smallSize = smallSize;
		if (smallSize) {
			currentIcon.setSize(currentIcon.getWidth() / 3f, currentIcon.getHeight() / 3f);
		}
		this.setTouchable(Touchable.enabled);
		addListener(new IconButtonClickListener(this));
		resetChildren();
	}

	public ImageButton(Drawable drawableImage, NinePatch backgroundNinePatch, boolean smallSize) {
		this.drawable = drawableImage;
		this.ninePatch = backgroundNinePatch;

		NinePatchDrawable buttonNinePatchDrawable = new NinePatchDrawable(backgroundNinePatch);
		setBackground(buttonNinePatchDrawable);

		this.currentIcon = new Image(drawableImage);
		this.smallSize = smallSize;
		if (smallSize) {
			currentIcon.setSize(currentIcon.getWidth() / 3f, currentIcon.getHeight() / 3f);
		}
		this.setTouchable(Touchable.enabled);
		addListener(new IconButtonClickListener(this));
		resetChildren();
	}

	public ImageButton clone() {
		ImageButton cloned;
		if (iconSprite != null) {
			cloned = new ImageButton(iconSprite, ninePatch, smallSize);
		} else {
			cloned = new ImageButton(drawable, ninePatch, smallSize);
		}
		cloned.defaultUnhighlightedColor = this.defaultUnhighlightedColor.cpy();
		cloned.defaultHighlightedColor = this.defaultHighlightedColor.cpy();
		cloned.currentBackgroundColor = this.currentBackgroundColor.cpy();
		cloned.action = this.action;
		cloned.isHighlighted = this.isHighlighted;
		cloned.isTogglable = this.isTogglable;
		cloned.toggledOn = this.toggledOn;
		cloned.highlight = this.highlight;
		return cloned;
	}

	private void resetChildren() {
		this.clearChildren();
		this.add(currentIcon).width(currentIcon.getWidth()).height(currentIcon.getHeight());
	}

	@Override
	public void act(float delta) {
		if (isTogglable && toggledOn) {
			currentBackgroundColor = defaultHighlightedColor.cpy();
			return;
		}

		if (isHighlighted && highlight < 1) {
			highlight += (HIGHLIGHT_CHANGE_SPEED * delta);
		} else if (!isHighlighted && highlight > 0) {
			highlight -= HIGHLIGHT_CHANGE_SPEED * delta;
		}

		if (highlight <= 0) {
			currentBackgroundColor = defaultUnhighlightedColor.cpy();
		} else if (highlight >= 1) {
			currentBackgroundColor = defaultHighlightedColor.cpy();
		} else {
			currentBackgroundColor.r = (defaultHighlightedColor.r * highlight) + (defaultUnhighlightedColor.r * (1 - highlight));
			currentBackgroundColor.g = (defaultHighlightedColor.g * highlight) + (defaultUnhighlightedColor.g * (1 - highlight));
			currentBackgroundColor.b = (defaultHighlightedColor.b * highlight) + (defaultUnhighlightedColor.b * (1 - highlight));
		}
	}

	@Override
	protected void drawBackground (Batch batch, float parentAlpha, float x, float y) {
		if (this.getBackground() == null) return;
		Color color = currentBackgroundColor;
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		getBackground().draw(batch, x, y, getWidth(), getHeight());
	}

	public boolean isTogglable() {
		return isTogglable;
	}

	public void setTogglable(boolean togglable) {
		isTogglable = togglable;
	}

	public void setAction(ButtonAction action) {
		this.action = action;
	}

	public boolean getToggledOn() {
		return toggledOn;
	}

	public void setToggledOn(boolean toggledOn) {
		this.toggledOn = toggledOn;
	}

	public Drawable getDrawable() {
		return drawable;
	}

	protected static class IconButtonClickListener extends ClickListener {

		protected final ImageButton parent;

		public IconButtonClickListener(ImageButton parent) {
			this.parent = parent;
		}

		public void clicked (InputEvent event, float x, float y) {
			if (parent.isTogglable) {
				parent.toggledOn = !parent.toggledOn;
			}
			if (parent.action == null) {
				Logger.error("No action set for ImageButton " + parent.currentIcon.getName());
			} else {
				parent.action.onClick();
			}
		}

		public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
			parent.isHighlighted = true;
		}

		public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
			parent.isHighlighted = false;
		}
	}

}
