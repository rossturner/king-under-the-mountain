package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.undermount.ui.i18n.I18nText;

public abstract class GameDialog implements Disposable {

	private final Skin uiSkin;
	private final MessageDispatcher messageDispatcher;
	protected Dialog dialog;
	protected Image fullScreenOverlay;

	public GameDialog(I18nText titleText, Skin uiSkin, MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
		dialog = new Dialog(titleText.toString(), uiSkin) {
			public void result(Object obj) {
				if (obj instanceof Runnable) {
					((Runnable)obj).run();
				}
				if (fullScreenOverlay != null) {
					fullScreenOverlay.remove();
				}
				dispose();
			}
		};
		this.uiSkin = uiSkin;

		fullScreenOverlay = new Image(uiSkin, "default-rect");
		fullScreenOverlay.setFillParent(true);
		fullScreenOverlay.setColor(1, 1, 1, 0.6f);
	}

	public void show(Stage stage) {
		if (fullScreenOverlay != null) {
			stage.addActor(fullScreenOverlay);
		}
		dialog.show(stage);
	}

	public GameDialog withText(I18nText descriptionText) {
		dialog.getContentTable().add(new I18nTextWidget(descriptionText, uiSkin, messageDispatcher)).row();
		return this;
	}

	public GameDialog withButton(I18nText buttonText) {
		dialog.button(buttonText.toString());
		return this;
	}

	public GameDialog withButton(I18nText buttonText, Runnable runnable) {
		dialog.button(buttonText.toString(), runnable);
		return this;
	}

}
