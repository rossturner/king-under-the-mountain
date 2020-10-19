package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import technology.rocketjump.undermount.ui.i18n.I18nText;

public class ModalDialog extends GameDialog {

	public ModalDialog(I18nText titleText, I18nText descriptionText, I18nText buttonText, Skin uiSkin, MessageDispatcher messageDispatcher) {
		super(titleText, uiSkin, messageDispatcher);
		withText(descriptionText);
		withButton(buttonText);
	}

	@Override
	public void dispose() {

	}
}
