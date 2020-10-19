package technology.rocketjump.undermount.launcher.translationupdater;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.ui.i18n.LanguageType;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationUpdater {

	private Map<String, Map<String, String>> combinedKeysToLanguagesToValues = new LinkedHashMap<>();
	private final AmazonTranslator amazonTranslator = new AmazonTranslator();
	private static final boolean USE_AWS_TRANSLATE = false;

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException("This class must be passed two arguments: the english translation directory and the target translations directory");
		}
		Path sourceDirectory = Paths.get(args[0]);
		Path targetDirectory = Paths.get(args[1]);
		new TranslationUpdater().process(sourceDirectory, targetDirectory);
	}

	private void process(Path sourceDirectory, Path targetFilePath) throws IOException {
		Path masterCsvPath = sourceDirectory.resolve("translations.csv");
		if (!Files.exists(masterCsvPath)) {
			throw new RuntimeException("Expecting to find " + masterCsvPath.toString());
		}

		Path targetLanguagesJsonPath = targetFilePath.resolve("languages.json");
		if (!Files.exists(targetLanguagesJsonPath)) {
			throw new RuntimeException("Expecting to find " + targetLanguagesJsonPath.toString());
		}

		ObjectMapper objectMapper = new ObjectMapper();
		List<LanguageType> targetLanguages = objectMapper.readValue(FileUtils.readFileToString(targetLanguagesJsonPath.toFile(), "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, LanguageType.class));


		parseMasterCsv(masterCsvPath, targetLanguages);


		for (LanguageType targetLanguage : targetLanguages) {
			CSVParser targetCsv = CSVParser.parse(targetFilePath.resolve(targetLanguage.getFilename()).toFile(),
					Charset.forName("UTF-8"), CSVFormat.DEFAULT.withFirstRecordAsHeader());
			Map<String, Integer> headerMap = targetCsv.getHeaderMap();

			for (CSVRecord csvRecord : targetCsv.getRecords()) {
				String key = csvRecord.get(headerMap.get("KEY"));
				if (key == null || key.isEmpty()) {
					continue;
				}
				key = key.toUpperCase();

				Map<String, String> languagesToValues = combinedKeysToLanguagesToValues.get(key);
				if (languagesToValues == null) {
					Logger.warn("No entry for key " + key + " while processing " + targetLanguage.getFilename());
				} else {
					String value = csvRecord.get(targetLanguage.getLabelEn().toUpperCase());
					if (value != null && !value.isEmpty()) {
						languagesToValues.put(targetLanguage.getCode(), value);
					}
				}
			}

			targetCsv.close();
		}

		// Output back to target directory
		for (LanguageType targetLanguage : targetLanguages) {
			output(targetLanguage, targetFilePath);
		}
	}

	private void parseMasterCsv(Path masterCsvPath, List<LanguageType> targetLanguages) throws IOException {
		CSVParser masterCsv = CSVParser.parse(masterCsvPath.toFile(), StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader());
		Map<String, Integer> masterHeaderMap = masterCsv.getHeaderMap();


		for (CSVRecord csvRecord : masterCsv.getRecords()) {
			String key = csvRecord.get(masterHeaderMap.get("KEY"));
			if (key == null || key.isEmpty()) {
				continue;
			}
			key = key.toUpperCase();

			String englishValue = csvRecord.get(masterHeaderMap.get("ENGLISH"));
			String notesValue = csvRecord.get(masterHeaderMap.get("NOTES"));

			Map<String, String> languagesToValuesForKey = new LinkedHashMap<>();
			languagesToValuesForKey.put("en", englishValue);
			languagesToValuesForKey.put("notes", notesValue);

			for (LanguageType targetLanguage : targetLanguages) {
				if (masterHeaderMap.containsKey(targetLanguage.getLabelEn().toUpperCase())) {
					String value = csvRecord.get(targetLanguage.getLabelEn().toUpperCase());
					if (value != null && !value.isEmpty()) {
						languagesToValuesForKey.put(targetLanguage.getCode(), value);
					}
				}
			}

			combinedKeysToLanguagesToValues.put(key, languagesToValuesForKey);
		}

		masterCsv.close();
	}

	List<String> skipLanguages = Arrays.asList();

	private void output(LanguageType targetLanguage, Path targetFilePath) throws IOException {
		if (skipLanguages.contains(targetLanguage.getLabelEn())) {
			return;
		}

		Path targetPath = targetFilePath.resolve(targetLanguage.getFilename());
		Files.deleteIfExists(targetPath);
		Files.createFile(targetPath);

		CSVPrinter printer = new CSVPrinter(new FileWriter(targetPath.toFile(), StandardCharsets.UTF_8), CSVFormat.DEFAULT);

		// Write first line
		printer.print("KEY");
		printer.print("NOTES");
		printer.print("ENGLISH");
		printer.print(targetLanguage.getLabelEn().toUpperCase());
		printer.println();

		List<String> allKeys = new ArrayList<>(combinedKeysToLanguagesToValues.keySet());
		Collections.sort(allKeys);


		String lastKey = "";
		for (String key : allKeys) {
			if (!firstPart(key).equals(firstPart(lastKey))) {
				printer.println();
			}
			printer.print(key);

			Map<String, String> languageToValueMap = combinedKeysToLanguagesToValues.get(key);

			printer.print(languageToValueMap.get("notes"));
			String englishValue = languageToValueMap.get("en");
			printer.print(englishValue);

			String value = languageToValueMap.get(targetLanguage.getCode());
			if (value == null) {
				value = "";
				if (useAwsTranslate(targetLanguage) && englishValue.length() > 0) {
					value = amazonTranslator.getTranslation(englishValue, "en", targetLanguage.getCode());
					value = fixReplacements(value, englishValue);
				}
			}
			printer.print(value);

			printer.println();
			lastKey = key;
		}

		printer.flush();
		IOUtils.closeQuietly(printer);
	}

	public static String fixReplacements(String translatedValue, String englishValue) {
		String originalTranslated = translatedValue;

		if (englishValue.contains("{{")) {
			translatedValue = addMissingRightBrackets(translatedValue);
			translatedValue = addMissingLeftBrackets(translatedValue);
			Pattern pattern = Pattern.compile("\\{\\{[^\\s]*}}");
			Matcher englishMatcher = pattern.matcher(englishValue);
			Matcher translatedMatcher = pattern.matcher(translatedValue);

			int numToSkip = 0;

			while (englishMatcher.find()) {
				boolean alsoMatched = translatedMatcher.find();
				if (!alsoMatched) {
					Logger.error("Could not find enough matches for " + englishValue + " in " + translatedValue);
					return translatedValue;
				}


				StringBuilder newTranslated = new StringBuilder();
				newTranslated.append(translatedValue.substring(0, translatedMatcher.start()));
				newTranslated.append(englishMatcher.group());
				newTranslated.append(translatedValue.substring(translatedMatcher.end(), translatedValue.length()));
				translatedValue = newTranslated.toString();

				numToSkip++;
				englishMatcher = pattern.matcher(englishValue);
				translatedMatcher = pattern.matcher(translatedValue);
				for (int i = 0; i < numToSkip; i++) {
					englishMatcher.find();
					translatedMatcher.find();
				}
			}
		}
		if (englishValue.contains("{{")) {
			Logger.info("Fixed " + originalTranslated + " to " + translatedValue);
		}
		return translatedValue;
	}

	private static String addMissingRightBrackets(String translatedValue) {
		Pattern rightSingleBracket = Pattern.compile("[^}](})[^}]");
		Matcher matcher = rightSingleBracket.matcher(translatedValue);
		if (matcher.find()) {
			StringBuilder newValue = new StringBuilder();
			newValue.append(translatedValue.substring(0, matcher.start() + 1));
			newValue.append("}");
			newValue.append(translatedValue.substring(matcher.start() + 1));

			translatedValue = newValue.toString();
			return addMissingRightBrackets(translatedValue);
		}
		return translatedValue;
	}

	private static String addMissingLeftBrackets(String translatedValue) {
		Pattern leftSingleBracket = Pattern.compile("[^\\{](\\{)[^\\{]");
		Matcher matcher = leftSingleBracket.matcher(translatedValue);
		if (matcher.find()) {
			StringBuilder newValue = new StringBuilder();
			newValue.append(translatedValue.substring(0, matcher.start() + 1));
			newValue.append("{");
			newValue.append(translatedValue.substring(matcher.start() + 1));

			translatedValue = newValue.toString();
			return addMissingLeftBrackets(translatedValue);
		}
		return translatedValue;
	}

	private static List<String> autotranslatedLanguages = Arrays.asList("German", "French", "Italian", "Spanish", "Portuguese",
			"Polish", "Russian", "Danish", "Swedish", "Japanese", "Simplified Chinese", "Korean");

	private boolean useAwsTranslate(LanguageType targetLanguage) {
		return USE_AWS_TRANSLATE && autotranslatedLanguages.contains(targetLanguage.getLabelEn());
	}

	private String firstPart(String key) {
		if (key.indexOf('.') > 0) {
			return key.substring(0, key.indexOf('.'));
		} else {
			return key;
		}
	}
}
