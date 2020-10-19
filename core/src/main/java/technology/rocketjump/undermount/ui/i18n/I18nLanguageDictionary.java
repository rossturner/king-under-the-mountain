package technology.rocketjump.undermount.ui.i18n;

import org.pmw.tinylog.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds all the words for a specific language
 */
public class I18nLanguageDictionary {

	private final Map<String, I18nWord> dictionary = new ConcurrentHashMap<>();
	private final String label;
	private final String labelEn;
	private final String iconName;
	private I18nLanguageDictionary fallbackDictionary;
	private boolean completeTranslation;

	public I18nLanguageDictionary(String label, String labelEn, String iconName) {
		this.label = label;
		this.labelEn = labelEn;
		this.iconName = iconName;
	}

	public String getLabel() {
		return label;
	}

	public String getLabelEn() {
		return labelEn;
	}

	public String getIconName() {
		return iconName;
	}

	public boolean containsKey(String key) {
		return dictionary.containsKey(key);
	}

	public I18nWord getWord(String key) {
		I18nWord i18nWord = dictionary.get(key);
		if (i18nWord == null) {
			if (fallbackDictionary != null) {
				Logger.warn("No translation for key " + key + " in language " + this.labelEn);
				i18nWord = fallbackDictionary.getWord(key);
			} else {
				i18nWord = new I18nWord(key);
			}
		}
		return i18nWord;
	}

	public void setFallbackDictionary(I18nLanguageDictionary fallbackDictionary) {
		this.fallbackDictionary = fallbackDictionary;
	}

	public Collection<I18nWord> getAllWords() {
		return dictionary.values();
	}

	public void addWord(I18nKey key, String translatedValue) {
		I18nWord i18nWord = dictionary.computeIfAbsent(key.key, I18nWord::new);
		i18nWord.add(key, translatedValue);
	}

	public boolean isCompleteTranslation() {
		return completeTranslation;
	}

	public void setCompleteTranslation(boolean completeTranslation) {
		this.completeTranslation = completeTranslation;
	}
}
