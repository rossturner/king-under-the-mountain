package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.widgets.tooltips.I18nTextElement;

public class I18nTextWidget extends VerticalGroup {

	public static final Color TOOLTIP_COLOR = HexColors.get("#bbf3ec");
	public static final Color DEFAULT_COLOR = Color.WHITE;
	public static final Color ERROR_COLOR = Color.RED;
	private final Skin skin;
	private final MessageDispatcher messageDispatcher;

	private I18nText i18nText;

	private int alignment = Align.left;
	private boolean isError = false;

	public I18nTextWidget(I18nText text, Skin skin, MessageDispatcher messageDispatcher) {
		super();
		this.skin = skin;
		this.messageDispatcher = messageDispatcher;
		setI18nText(text);
	}

	public void setI18nText(I18nText text) {
		this.i18nText = text;
		reset();
	}

	private void reset() {
		clearChildren();
		if (i18nText == null) {
			return;
		}
		HorizontalGroup horizontalGroup = null;
		for (I18nTextElement i18nTextElement : i18nText.getElements()) {
			if (i18nTextElement.isLineBreak() && horizontalGroup != null) {
				horizontalGroup.align(alignment);
				addActor(horizontalGroup);
				horizontalGroup = null;
			} else {
				if (horizontalGroup == null) {
					horizontalGroup = new HorizontalGroup();
				}

				// add each element as new label, potentially with tooltip listener
				Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
				style.fontColor = i18nTextElement.getTooltipI18nKey() != null ? TOOLTIP_COLOR : DEFAULT_COLOR;
				if (isError) {
					style.fontColor = ERROR_COLOR;
				}
				Label label = new Label(i18nTextElement.getText(), style);
				if (i18nTextElement.getTooltipI18nKey() != null) {
					label.addListener(new TooltipListener(label, i18nTextElement.getTooltipI18nKey(), messageDispatcher));
				}
				label.setAlignment(alignment);
				horizontalGroup.addActor(label);

			}
		}

		if (horizontalGroup != null) {
			horizontalGroup.align(alignment);
			addActor(horizontalGroup);
		}
		this.columnAlign(alignment);
	}

	public I18nText getI18nText() {
		return i18nText;
	}

	public void setError(boolean error) {
		isError = error;
	}
}
