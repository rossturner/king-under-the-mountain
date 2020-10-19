package technology.rocketjump.undermount.ui.i18n;

import technology.rocketjump.undermount.entities.model.physical.humanoid.Gender;

import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.undermount.ui.i18n.I18nWordClass.UNSPECIFIED;

public class I18nKey {

	public final String key;
	public final I18nWordClass wordClass;
	public final Gender gender;

	public static final List<String> knownSuffixes;
	// "NOUN", "MALE_NOUN", "FEMALE_NOUN", "ADJECTIVE", "MALE_ADJECTIVE", "FEMALE_ADJECTIVE", "PLURAL", "MALE_PLURAL", "FEMALE_PLURAL", "VERB"
	static {
		knownSuffixes = new ArrayList<>();

		for (I18nWordClass wordClass : I18nWordClass.values()) {
			if (wordClass.equals(UNSPECIFIED)) {
				continue;
			}
			knownSuffixes.add("." + Gender.MALE.name() + "_" + wordClass.name());
			knownSuffixes.add("." + Gender.FEMALE.name() + "_" + wordClass.name());
			knownSuffixes.add("." + wordClass.name());
		}
	}


	I18nKey(String keyString) {
		for (String knownSuffix : knownSuffixes) {
			if (keyString.endsWith(knownSuffix)) {
				this.key = keyString.substring(0, keyString.lastIndexOf(knownSuffix));
				String suffix = keyString.substring(keyString.lastIndexOf(knownSuffix) + 1, keyString.length());
				if (suffix.contains("_")) {
					String[] split = suffix.split("_");
					gender = Gender.valueOf(split[0]);
					wordClass = I18nWordClass.valueOf(split[1]);
				} else {
					gender = Gender.ANY;
					wordClass = I18nWordClass.valueOf(suffix);
				}
				return;
			}
		}
		this.wordClass = UNSPECIFIED;
		this.gender = Gender.ANY;
		this.key = keyString;
	}
}
