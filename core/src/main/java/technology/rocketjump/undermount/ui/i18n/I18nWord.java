package technology.rocketjump.undermount.ui.i18n;

import technology.rocketjump.undermount.entities.model.physical.creature.Gender;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the several forms of a word e.g. noun, verb
 * Optionally gendered depending on the acting entity
 */
public class I18nWord implements I18nString {

	public static final I18nWord BLANK = new I18nWord("");
	private static final List<I18nWordClass> priorityOfDefault =
			Arrays.asList(I18nWordClass.NOUN, I18nWordClass.VERB, I18nWordClass.ADJECTIVE, I18nWordClass.UNSPECIFIED);

	private final String key;

	private Map<I18nWordClass, Map<Gender, String>> wordMap = new EnumMap<>(I18nWordClass.class);

	public I18nWord(String key) {
		this.key = key;
		for (I18nWordClass wordClass : I18nWordClass.values()) {
			wordMap.put(wordClass, new EnumMap<>(Gender.class));
		}
	}

	/**
	 * This is only used when a specific one-off word is used
	 */
	public I18nWord(String key, String value) {
		this(key);
		this.add(new I18nKey(key), value);
	}

	public void add(I18nKey key, String translatedValue) {
		Map<Gender, String> genderMap = wordMap.get(key.wordClass);
		genderMap.put(key.gender, translatedValue);
		genderMap.put(Gender.ANY, translatedValue);
	}

	public String getKey() {
		return key;
	}

	public String get() {
		return this.get(I18nWordClass.UNSPECIFIED, Gender.ANY);
	}

	public String get(I18nWordClass wordClass) {
		return this.get(wordClass, Gender.ANY);
	}

	public String get(I18nWordClass wordClass, Gender gender) {
		Map<Gender, String> byWordClass = wordMap.get(wordClass);
		if (byWordClass.isEmpty()) {
			for (I18nWordClass otherWordClass : priorityOfDefault) {
				byWordClass = wordMap.get(otherWordClass);
				if (!byWordClass.isEmpty()) {
					break;
				}
			}
		}

		if (!Gender.ANY.equals(gender) && !byWordClass.containsKey(gender)) {
			gender = Gender.ANY;
		}

		String value = byWordClass.get(gender);
		if (value == null) {
			return key;
		} else {
			return value;
		}
	}

	public boolean hasTooltip() {
		return !wordMap.get(I18nWordClass.TOOLTIP).isEmpty() && !wordMap.get(I18nWordClass.TOOLTIP).get(Gender.ANY).equals(wordMap.get(I18nWordClass.UNSPECIFIED).get(Gender.ANY));
	}

	@Override
	public String toString() {
		return get();
	}

}
