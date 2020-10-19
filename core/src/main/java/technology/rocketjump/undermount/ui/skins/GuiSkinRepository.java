package technology.rocketjump.undermount.ui.skins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.VisUI;
import technology.rocketjump.undermount.ui.fonts.FontRepository;

@Singleton
public class GuiSkinRepository {

	private final FontRepository fontRepository;
	private Skin uiSkin = new Skin(Gdx.files.internal("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json

	@Inject
	public GuiSkinRepository(FontRepository fontRepository) {
		this.fontRepository = fontRepository;

		fontChanged();

		if (!VisUI.isLoaded()) {
			VisUI.load();
		}
	}

	public Skin getDefault() {
		return uiSkin;
	}

	public void fontChanged() {
		BitmapFont bitmapFont = fontRepository.getDefaultFontForUI().getBitmapFont();
		uiSkin.add("default-font", bitmapFont);

		uiSkin.get(TextField.TextFieldStyle.class).font = bitmapFont;
		uiSkin.get(Label.LabelStyle.class).font = bitmapFont;
		uiSkin.get(CheckBox.CheckBoxStyle.class).font = bitmapFont;
		uiSkin.get(Window.WindowStyle.class).titleFont = bitmapFont;
		uiSkin.get(List.ListStyle.class).font = bitmapFont;
		uiSkin.get(SelectBox.SelectBoxStyle.class).font = bitmapFont;
		uiSkin.get(SelectBox.SelectBoxStyle.class).listStyle.font = bitmapFont;
		uiSkin.get(TextButton.TextButtonStyle.class).font = bitmapFont;
	}

}
