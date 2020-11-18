package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;

import java.util.ArrayList;
import java.util.List;

public class ToggleButtonSet extends HorizontalGroup {

	private final List<ImageButtonWithValue> buttons = new ArrayList<>();

	public ToggleButtonSet(Skin uiSkin, List<ToggleButtonDefinition> buttonDefinitions, ToggleButtonAction action) {
		TextButton.TextButtonStyle textButtonStyle = uiSkin.get(TextButton.TextButtonStyle.class);
		ImageButton.ImageButtonStyle imageButtonStyle = new ImageButton.ImageButtonStyle(
				textButtonStyle.up, textButtonStyle.down, textButtonStyle.checked,
				null, null, null
		);

		for (ToggleButtonDefinition buttonDefinition : buttonDefinitions) {
			ImageButtonWithValue imageButton = new ImageButtonWithValue(buttonDefinition.value, imageButtonStyle, new SpriteDrawable(buttonDefinition.iconSprite));
			this.buttons.add(imageButton);
			imageButton.addListener(new ToggleButtonClickListener(imageButton, buttons, action));
			imageButton.invalidateHierarchy();
			this.addActor(imageButton);
		}

		this.expand();
	}

	public String getChecked() {
		for (ImageButtonWithValue button : buttons) {
			if (button.isChecked()) {
				return button.value;
			}
		}
		return null;
	}

	public void setChecked(String value) {
		for (ImageButtonWithValue button : buttons) {
			if (button.value.equals(value)) {
				button.setChecked(true);
			} else {
				button.setChecked(false);
			}
		}
	}

	public static class ToggleButtonDefinition {

		public final String value;
		public final Sprite iconSprite;

		public ToggleButtonDefinition(String value, Sprite iconSprite) {
			this.value = value;
			this.iconSprite = iconSprite;
		}

	}

	private static class ToggleButtonClickListener extends ClickListener {

		private final ImageButtonWithValue parent;
		private final List<ImageButtonWithValue> allButtons;
		private final ToggleButtonAction action;

		public ToggleButtonClickListener(ImageButtonWithValue parent, List<ImageButtonWithValue> allButtons, ToggleButtonAction action) {
			this.parent = parent;
			this.allButtons = allButtons;
			this.action = action;
		}

		@Override
		public void clicked(InputEvent event, float x, float y) {
			super.clicked(event, x, y);
			for (ImageButtonWithValue otherButton : allButtons) {
				if (otherButton.value.equals(parent.value)) {
					otherButton.setChecked(true);
					action.buttonClicked(parent.value);
				} else {
					otherButton.setChecked(false);
				}
			}
		}
	}

	public interface ToggleButtonAction {

		void buttonClicked(String value);

	}

	private static class ImageButtonWithValue extends ImageButton {

		public final String value;

		public ImageButtonWithValue(String value, ImageButtonStyle baseStyle, SpriteDrawable spriteDrawable) {
			super(new ImageButtonStyle(baseStyle.up, baseStyle.down, baseStyle.down,
					spriteDrawable, spriteDrawable, spriteDrawable));
			this.value = value;
		}

	}

}
