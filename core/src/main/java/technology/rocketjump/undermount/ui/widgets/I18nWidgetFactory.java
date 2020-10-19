package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.ai.goap.EntityNeed;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.i18n.I18nWordClass;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static technology.rocketjump.undermount.entities.ai.goap.EntityNeed.*;

@Singleton
public class I18nWidgetFactory implements I18nUpdatable {

	private final I18nTranslator i18nTranslator;
	private final Skin uiSkin;

	private final Map<String, I18nLabel> labels = new HashMap<>();
	private final Map<String, I18nTextButton> buttons = new HashMap<>();
	private final Map<String, I18nCheckbox> checkboxes = new HashMap<>();

	@Inject
	public I18nWidgetFactory(I18nTranslator i18nTranslator, GuiSkinRepository guiSkinRepository) {
		this.i18nTranslator = i18nTranslator;
		this.uiSkin = guiSkinRepository.getDefault();
	}

	/**
	 * Note that these currently reuse a single instance so this does not allow for multiple of the same actor on the same stage
	 */
	public I18nLabel createLabel(String i18nKey) {
		return labels.computeIfAbsent(i18nKey, (key) -> new I18nLabel(key, i18nTranslator.getTranslatedString(key).toString(), uiSkin));
	}

	public I18nLabel createLabel(String i18nKey, I18nWordClass wordClass) {
		return labels.computeIfAbsent(i18nKey, (key) -> new I18nLabel(key, i18nTranslator.getTranslatedString(key, wordClass).toString(), uiSkin, wordClass));
	}

	public I18nTextButton createTextButton(String i18nKey) {
		return buttons.computeIfAbsent(i18nKey, (key) -> new I18nTextButton(key, i18nTranslator.getTranslatedString(key).toString(), uiSkin));
	}

	public I18nCheckbox createCheckbox(String i18nKey) {
		return checkboxes.computeIfAbsent(i18nKey, (key) -> new I18nCheckbox(key, i18nTranslator.getTranslatedString(key).toString(), uiSkin));
	}

	public Map<EntityNeed, I18nLabel> createNeedsLabels() {
		Map<EntityNeed, I18nLabel> needsLabels = new LinkedHashMap<>();
		for (EntityNeed need : Arrays.asList(SLEEP, FOOD, DRINK)) {
			needsLabels.put(need, createLabel(need.getI18nKey()));
		}
		return needsLabels;
	}

	@Override
	public void onLanguageUpdated() {
		for (I18nLabel i18nLabel : labels.values()) {
			i18nLabel.setStyle(i18nLabel.getStyle());
			i18nLabel.setText(i18nTranslator.getTranslatedString(i18nLabel.getI18nKey(), i18nLabel.getI18nWordClass()).toString());
		}
		for (I18nTextButton i18nTextButton : buttons.values()) {
			i18nTextButton.setStyle(i18nTextButton.getStyle());
			i18nTextButton.setText(i18nTranslator.getTranslatedString(i18nTextButton.getI18nKey(), i18nTextButton.getI18nWordClass()).toString());
		}
		for (I18nCheckbox i18nCheckbox : checkboxes.values()) {
			i18nCheckbox.setStyle(i18nCheckbox.getStyle());
			i18nCheckbox.setText(i18nTranslator.getTranslatedString(i18nCheckbox.getI18nKey(), i18nCheckbox.getI18nWordClass()).toString());
		}
	}
}
