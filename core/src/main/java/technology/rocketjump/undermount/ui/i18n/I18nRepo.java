package technology.rocketjump.undermount.ui.i18n;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.persistence.UserPreferences;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.csv.CSVFormat.DEFAULT;

/**
 * This class holds everything for each defined language
 */
@Singleton
public class I18nRepo {

	private final Map<String, I18nLanguageDictionary> dictionaries;
	private final List<LanguageType> languages;
	private I18nLanguageDictionary currentLanguageDictionary;
	private LanguageType currentLanguageType;

	@Inject
	public I18nRepo(UserPreferences userPreferences) throws IOException {
		File languagesCsvFile = new File("assets/translations/collated.csv");
		File languagesJsonFile = new File("assets/translations/languages.json");

		Map<String, I18nLanguageDictionary> dictionaries = new ConcurrentHashMap<>();

		ObjectMapper objectMapper = new ObjectMapper();
		List<LanguageType> expectedLanguages = objectMapper.readValue(FileUtils.readFileToString(languagesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, LanguageType.class));
		expectedLanguages.removeIf(languageType -> !languageType.isEnabled());

		for (LanguageType languageType : expectedLanguages) {
			dictionaries.put(languageType.getCode(), new I18nLanguageDictionary(languageType.getLabel(),
					languageType.getLabelEn(), languageType.getIcon()));
		}

		for (LanguageType languageType : expectedLanguages) {
			I18nLanguageDictionary dictionary = dictionaries.get(languageType.getCode());
			if (dictionary != null && languageType.getFallback() != null) {
				dictionary.setFallbackDictionary(dictionaries.get(languageType.getFallback()));
			}
		}


		CSVParser parsedCsv = CSVParser.parse(languagesCsvFile, Charset.forName("UTF-8"), DEFAULT);
		Map<String, Integer> columnIndices = new HashMap<>();

		for (CSVRecord csvRecord : parsedCsv.getRecords()) {
			if (columnIndices.isEmpty()) {
				for (int cursor = 0; cursor < csvRecord.size(); cursor++) {
					columnIndices.put(csvRecord.get(cursor).toUpperCase(), cursor);
				}
			} else {
				String key = csvRecord.get(columnIndices.get("KEY"));
				if (key == null || key.isEmpty()) {
					continue;
				}
				key = key.toUpperCase();
				for (I18nLanguageDictionary dictionary : dictionaries.values()) {
					Integer index = columnIndices.get(dictionary.getLabelEn().toUpperCase());
					if (index != null) {
						String translatedValue = csvRecord.get(index);
						if (translatedValue == null || translatedValue.isEmpty()) {
//						Logger.error("Could not find language entry for key " + key);
							continue;
						} else {
							translatedValue = translatedValue.trim();
							translatedValue = translatedValue.replace("\\n", "\n");

							addValueTo(dictionary, key, translatedValue);
						}
					}
				}
			}
		}

		this.dictionaries = dictionaries;
		this.languages = expectedLanguages;

		String preferenceLangCode = userPreferences.getPreference(UserPreferences.PreferenceKey.LANGUAGE, "en-gb");
		String langCode = dictionaries.containsKey(preferenceLangCode) ? preferenceLangCode : "en-gb";

		currentLanguageDictionary = this.dictionaries.get(langCode);
		this.dictionaries.get("en-gb").setCompleteTranslation(true);

		currentLanguageType = languages.stream().filter(lang -> lang.getCode().equalsIgnoreCase(langCode)).findFirst().orElse(languages.get(0));

		Collection<I18nWord> allWords = currentLanguageDictionary.getAllWords();

		for (I18nLanguageDictionary dictionary : dictionaries.values()) {
			if (dictionary.getLabelEn().equalsIgnoreCase("english")) {
				continue;
			}

			boolean isCompleteTranslation = true;

			for (I18nWord expectedWord : allWords) {
				if (!dictionary.containsKey(expectedWord.getKey())) {
					isCompleteTranslation = false;
					break;
				}
			}

			dictionary.setCompleteTranslation(isCompleteTranslation);
		}
	}

	private boolean initialised = false;

	public void init(TextureAtlasRepository textureAtlasRepository) {
		if (initialised) {
			return;
		}

		TextureAtlas textureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);

		for (LanguageType languageType : languages) {
			languageType.setIconSprite(textureAtlas.createSprite(languageType.getIcon()));
		}
		initialised = true;
	}

	public List<LanguageType> getAllLanguages() {
		if (!initialised) {
			throw new RuntimeException("Not yet initialised: " + this.getClass().getSimpleName());
		}
		return languages;
	}

	public I18nLanguageDictionary getCurrentLanguage() {
		return currentLanguageDictionary;
	}

	public LanguageType getCurrentLanguageType() {
		return currentLanguageType;
	}

	public void setCurrentLanguage(LanguageType languageType) {
		this.currentLanguageType = languageType;
		currentLanguageDictionary = dictionaries.get(languageType.getCode());
	}


	private void addValueTo(I18nLanguageDictionary dictionary, String keyString, String translatedValue) {
		I18nKey key = new I18nKey(keyString);
		dictionary.addWord(key, translatedValue);
	}

	// This is some serious inner loops
	public String getAllCharacters(String defaultChars) {
		Set<String> allCharacters = new TreeSet<>();

		for (int cursor = 0; cursor < defaultChars.length(); cursor++) {
			allCharacters.add(String.valueOf(defaultChars.charAt(cursor)));
		}

		for (I18nLanguageDictionary dictionary : dictionaries.values()) {
			for (I18nWord word : dictionary.getAllWords()) {
				for (I18nWordClass wordClass : I18nWordClass.values()) {
					String wordValue = word.get(wordClass);
					if (wordValue != null) {
						for (int cursor = 0; cursor < wordValue.length(); cursor++) {
							char character = wordValue.charAt(cursor);
							allCharacters.add(String.valueOf(character));
						}
					}
				}
			}
		}

		StringBuilder result = new StringBuilder();
		for (String allCharacter : allCharacters) {
			result.append(allCharacter);
		}
		return result.toString();
	}
}
