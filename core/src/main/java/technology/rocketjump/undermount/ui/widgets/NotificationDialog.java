package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import technology.rocketjump.undermount.ui.i18n.I18nText;

public class NotificationDialog extends GameDialog {

	private Texture texture;
	private Image image;

	public NotificationDialog(I18nText titleText, Skin uiSkin, MessageDispatcher messageDispatcher) {
		super(titleText, uiSkin, messageDispatcher);
		dialog.getContentTable().pad(4);
		dialog.getButtonTable().pad(4).padBottom(8);
	}

	@Override
	public void dispose() {
		if (texture != null) {
			texture.dispose();
		}
	}

	public Table getContentTable() {
		return dialog.getContentTable();
	}

	public void addTexture(Texture texture) {
		this.texture = texture;
		this.image = new Image(texture);
		dialog.getContentTable().add(image).pad(8).row();
	}
}
