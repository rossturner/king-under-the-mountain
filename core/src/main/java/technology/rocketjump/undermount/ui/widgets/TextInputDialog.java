package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import technology.rocketjump.undermount.ui.i18n.I18nText;

public class TextInputDialog extends GameDialog {

	public final TextField inputBox;

	public TextInputDialog(I18nText titleText, I18nText descriptionText, String inputPlaceholder,
						   I18nText buttonText, Skin uiSkin, TextInputDialogCallback onButtonClick, MessageDispatcher messageDispatcher) {
		super(titleText, uiSkin, messageDispatcher);
//		withText(descriptionText);
		inputBox = new TextField(inputPlaceholder, uiSkin);
		dialog.getContentTable().add(inputBox).row();
		withButton(buttonText, () -> {
			onButtonClick.onOkButtonClick(inputBox.getText());
		});
	}

	@Override
	public void show(Stage stage) {
		super.show(stage);
		stage.setKeyboardFocus(inputBox);
		inputBox.selectAll();
	}


	@Override
	public void dispose() {
		
	}

	public interface TextInputDialogCallback {

		void onOkButtonClick(String text);

	}
}
