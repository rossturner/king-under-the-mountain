package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import technology.rocketjump.undermount.ui.i18n.I18nWordClass;

public class I18nCheckbox extends CheckBox {

	private final String i18nKey;
	private final I18nWordClass i18nWordClass;

	public I18nCheckbox(String i18nKey, String i18nValue, Skin skin) {
		this(i18nKey, i18nValue, skin, I18nWordClass.UNSPECIFIED);
	}

	public I18nCheckbox(String i18nKey, String i18nValue, Skin skin, I18nWordClass i18nWordClass) {
		super(i18nValue, skin);
		this.i18nKey = i18nKey;
		this.i18nWordClass = i18nWordClass;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public I18nWordClass getI18nWordClass() {
		return i18nWordClass;
	}
}
