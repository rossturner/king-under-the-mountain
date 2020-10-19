package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import technology.rocketjump.undermount.ui.i18n.I18nWordClass;

public class I18nLabel extends Label {

	private final String i18nKey;
	private final Skin skin;
	private final I18nWordClass i18nWordClass;

	public I18nLabel(String i18nKey, String i18nValue, Skin skin) {
		this(i18nKey, i18nValue, skin, I18nWordClass.UNSPECIFIED);
	}

	public I18nLabel(I18nLabel other) {
		this(other.i18nKey, other.getText().toString(), other.skin);
	}

	public I18nLabel(String key, String translatedString, Skin uiSkin, I18nWordClass wordClass) {
		super(translatedString, uiSkin);
		this.skin = uiSkin;
		this.i18nKey = key;
		this.i18nWordClass = wordClass;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public I18nWordClass getI18nWordClass() {
		return i18nWordClass;
	}
}
