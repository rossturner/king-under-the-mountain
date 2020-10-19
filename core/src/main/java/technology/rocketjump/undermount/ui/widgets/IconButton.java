package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.ui.actions.ButtonAction;
import technology.rocketjump.undermount.ui.fonts.GameFont;
import technology.rocketjump.undermount.ui.i18n.I18nText;

// TODO extended button/table functionality from ImageButton/IconButton/ClickableTable needs refactoring together
public class IconButton extends Table {

	public static final int ICON_PX_WIDTH_HEIGHT = 64;
	public static final float MARGIN = 2;
	public static final float HIGHLIGHT_CHANGE_SPEED = 2f; // 1 / How many seconds it takes to change color
	public static final float DEFAULT_TEXT_HEIGHT = 24;
	public static final float TEXT_MARGIN = 2;

	private final long iconButtonId;
	private GameFont defaultFont;
	private final String i18nKey;
	private GameFont currentFont;
	private final ButtonStyle style;

	private Image currentIcon;
	private Label currentLabel;
	private Color defaultBackgroundColor = Color.BLACK;
	private Color defaultForegroundColor = Color.GREEN;
	private Color currentBackgroundColor = new Color();
	private Color currentForegroundColor = new Color();
	private NinePatchDrawable buttonNinepatch;
	private ButtonAction action;
	private ButtonAction rightClickAction;
	private ButtonAction onClickSoundAction;
	private IconButtonOnEnter onEnter;
	private boolean isHighlighted;
	private float highlight = 0;
	private TooltipListener tooltipListener;

	public IconButton(GameFont font, String i18nKey) {
		this(font, i18nKey, ButtonStyle.DEFAULT);
	}

	public IconButton(GameFont font, String i18nKey, ButtonStyle style) {
		this.iconButtonId = SequentialIdGenerator.nextId();
		this.defaultFont = font;
		this.currentFont = font;
		this.style = style;
		this.i18nKey = i18nKey;
		this.setTouchable(Touchable.enabled);

		addListener(new IconButtonClickListener(this));
	}

	public void setLabelText(I18nText labelText, MessageDispatcher messageDispatcher) {
		if (tooltipListener != null) {
			this.removeListener(tooltipListener);
			this.tooltipListener = null;
		}
		if (labelText.getFirstTooltip() != null) {
			this.tooltipListener = new TooltipListener(this, labelText.getFirstTooltip(), messageDispatcher);
			this.addListener(tooltipListener);
		}

		String label = labelText.toString();
		label = wrap(label);
		this.currentFont = defaultFont;
		resetLabel(label);
		trimLabel(label);
		resetChildren();
	}

	private String wrap(String labelText) {
		if (style.equals(ButtonStyle.LARGE)) {
			if (labelText.indexOf('\n') == -1 && labelText.indexOf(' ') != -1) {
				int indexOfMiddleSpace = -1;
				int indexOfMiddle = labelText.length() / 2;
				int lastDistanceToMiddle = labelText.length();
				for (int cursor = 0; cursor < labelText.length(); cursor++) {
					if (labelText.charAt(cursor) == ' ') {
						int distanceToMiddle = Math.abs(indexOfMiddle - cursor);
						if (distanceToMiddle < lastDistanceToMiddle) {
							indexOfMiddleSpace = cursor;
							lastDistanceToMiddle = distanceToMiddle;
						}
					}
				}

				if (indexOfMiddleSpace != -1) {
					return labelText.substring(0, indexOfMiddleSpace) + "\n" + labelText.substring(indexOfMiddleSpace + 1, labelText.length());
				}
			}
		}
		return labelText;
	}

	private void resetLabel(String labelText) {
		Label.LabelStyle labelStyle = new Label.LabelStyle(currentFont.getBitmapFont(), Color.WHITE);
		this.currentLabel = new Label(labelText, labelStyle);
		if (this.currentLabel.getPrefWidth() > style.MAX_WIDTH && currentFont.getSmaller() != null) {
			this.currentFont = currentFont.getSmaller();
			resetLabel(labelText);
		}
	}

	private void trimLabel(String labelText) {
		float maxAllowedWidth = style.MAX_WIDTH;
		float currentLabelWidth = currentLabel.getPrefWidth();
		while (currentLabelWidth > maxAllowedWidth) {
			labelText = trimLastCharOfEachLine(labelText);
			Label.LabelStyle labelStyle = new Label.LabelStyle(currentFont.getBitmapFont(), Color.WHITE);
			this.currentLabel = new Label(labelText, labelStyle);
			currentLabelWidth = currentLabel.getPrefWidth();
		}
	}

	private String trimLastCharOfEachLine(String text) {
		text = text.replaceAll(".\\\n", "\n");
		return text.substring(0, text.length() - 1);
	}

	private void resetChildren() {
		this.clearChildren();
		if (currentIcon != null) {
			Cell cell = this.add(currentIcon);
			if (style.equals(ButtonStyle.LARGE)) {
				cell.padLeft(ICON_PX_WIDTH_HEIGHT / 2).padRight(ICON_PX_WIDTH_HEIGHT / 2);
			} else if (style.equals(ButtonStyle.SMALL) || style.equals(ButtonStyle.HALF_SIZE_NO_TEXT)) {
				cell.height(currentIcon.getHeight()).width(currentIcon.getWidth());
			}

			if (!style.equals(ButtonStyle.SMALL)) {
				this.row();
			} else {
				cell.padRight(MARGIN);
			}
		}
		if (currentLabel != null) {
			this.add(currentLabel).height(style.LABEL_HEIGHT);
			if (!style.equals(ButtonStyle.SMALL)) {
				this.row();
			}
		}
		if (style.equals(ButtonStyle.EXTRA_WIDE)) {
			this.add(new Table()).width(300);
		}
	}

	public void setIconSprite(Sprite iconSprite) {
		this.currentIcon = new Image(iconSprite);
		if (this.style.equals(ButtonStyle.SMALL) || this.style.equals(ButtonStyle.HALF_SIZE_NO_TEXT)) {
			currentIcon.setSize(currentIcon.getWidth() / 2f, currentIcon.getHeight() / 2f);
		}
		resetChildren();
	}

	@Override
	public void act(float delta) {
		if (isHighlighted && highlight < 1) {
			highlight += (HIGHLIGHT_CHANGE_SPEED * delta);
		} else if (!isHighlighted && highlight > 0) {
			highlight -= HIGHLIGHT_CHANGE_SPEED * delta;
		}

		if (highlight <= 0) {
			currentBackgroundColor = defaultBackgroundColor.cpy();
			currentForegroundColor = defaultForegroundColor.cpy();
		} else if (highlight >= 1) {
			currentBackgroundColor = defaultForegroundColor.cpy();
			currentForegroundColor = defaultBackgroundColor.cpy();
		} else {
			currentForegroundColor.r = (defaultBackgroundColor.r * highlight) + (defaultForegroundColor.r * (1 - highlight));
			currentForegroundColor.g = (defaultBackgroundColor.g * highlight) + (defaultForegroundColor.g * (1 - highlight));
			currentForegroundColor.b = (defaultBackgroundColor.b * highlight) + (defaultForegroundColor.b * (1 - highlight));

			currentBackgroundColor.r = (defaultForegroundColor.r * highlight) + (defaultBackgroundColor.r * (1 - highlight));
			currentBackgroundColor.g = (defaultForegroundColor.g * highlight) + (defaultBackgroundColor.g * (1 - highlight));
			currentBackgroundColor.b = (defaultForegroundColor.b * highlight) + (defaultBackgroundColor.b * (1 - highlight));
		}

		if (currentIcon != null) {
			this.currentIcon.setColor(currentForegroundColor);
		}
	}

	@Override
	protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
		if (this.getBackground() == null) return;
		Color color = currentBackgroundColor;
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		getBackground().draw(batch, x, y, getWidth(), getHeight());
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.defaultBackgroundColor = backgroundColor.cpy();
	}

	public Color getBackgroundColor() {
		return defaultBackgroundColor;
	}

	public void setForegroundColor(Color foregroundColor) {
		this.defaultForegroundColor = foregroundColor.cpy();
	}

	public Color getForegroundColor() {
		return defaultForegroundColor;
	}

	public void setButtonNinepatch(NinePatch ninePatch) {
		this.buttonNinepatch = new NinePatchDrawable(ninePatch);
		setBackground(buttonNinepatch);
	}

	public NinePatchDrawable getButtonNinepatch() {
		return buttonNinepatch;
	}

	public void setAction(ButtonAction action) {
		this.action = action;
	}

	public void setRightClickAction(ButtonAction rightClickAction) {
		this.rightClickAction = rightClickAction;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setOnEnter(IconButtonOnEnter onEnter) {
		this.onEnter = onEnter;
	}

	public void setOnClickSoundAction(ButtonAction onClickSoundAction) {
		this.onClickSoundAction = onClickSoundAction;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		IconButton that = (IconButton) o;
		return iconButtonId == that.iconButtonId;
	}

	@Override
	public int hashCode() {
		return (int) iconButtonId;
	}

	public com.badlogic.gdx.utils.StringBuilder getText() {
		if (currentLabel != null) {
			return currentLabel.getText();
		} else {
			return null;
		}
	}

	public void setFont(GameFont font) {
		this.defaultFont = font;
		this.currentFont = font;
	}

	protected static class IconButtonClickListener extends ClickListener {

		protected final IconButton parent;

		public IconButtonClickListener(IconButton parent) {
			this.parent = parent;
		}

		@Override
		public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
			if (button == Input.Buttons.RIGHT && parent.rightClickAction != null) {
				parent.rightClickAction.onClick();
				if (parent.onClickSoundAction != null) {
					parent.onClickSoundAction.onClick();
				}
			}
			return super.touchDown(event, x, y, pointer, button);
		}

		public void clicked(InputEvent event, float x, float y) {
			if (parent.onClickSoundAction != null) {
				parent.onClickSoundAction.onClick();
			}
			if (parent.action == null) {
				Logger.error("No action set for button " + parent.currentLabel);
			} else {
				parent.action.onClick();
			}
		}

		public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
			if (fromActor == null || !fromActor.isDescendantOf(parent)) {
				if (parent.onEnter != null && !parent.isHighlighted) {
					parent.onEnter.onEnterAction();
				}
				parent.isHighlighted = true;
			}
		}

		public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
			if (toActor == null || !toActor.isDescendantOf(parent)) {
				parent.isHighlighted = false;
			}
		}

	}

	public interface IconButtonOnEnter {

		void onEnterAction();

	}

}
