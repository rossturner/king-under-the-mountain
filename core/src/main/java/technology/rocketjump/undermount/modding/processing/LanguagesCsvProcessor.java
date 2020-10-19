package technology.rocketjump.undermount.modding.processing;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.modding.exception.ModLoadingException;
import technology.rocketjump.undermount.modding.model.ModArtifact;
import technology.rocketjump.undermount.modding.model.ModArtifactDefinition;
import technology.rocketjump.undermount.modding.model.ParsedMod;
import technology.rocketjump.undermount.ui.i18n.LanguageType;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

public class LanguagesCsvProcessor extends ModArtifactProcessor {

	private List<LanguageType> languages;
	private Map<String, Map<String, String>> combinedKeysToLanguagesToValues = new LinkedHashMap<>();

	public LanguagesCsvProcessor(ModArtifactDefinition definition) {
		super(definition);

	}

	@Override
	public void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException {
		Map<String, Map<String, String>> keysToLanguagesToValues = new LinkedHashMap<>();

		// Assuming this is processed after translations/languages.json
		try {
			initLanguages(mod.getBasePath());
		} catch (IOException e) {
			throw new ModLoadingException(e);
		}



			for (LanguageType language : languages) {
				try {
					if (!language.isEnabled() || language.getLabelEn().equalsIgnoreCase("notes")) {
						continue;
					}

					Path csvFile = null;
					for (Path sourceFile : modArtifact.sourceFiles) {
						if (sourceFile.getFileName().toString().equalsIgnoreCase(language.getFilename())) {
							csvFile = sourceFile;
							break;
						}
					}

					if (csvFile == null) {
						Logger.error("Can not find file to parse: " + language.getFilename());
						continue;
					}

					CSVParser parsedCsv = CSVParser.parse(csvFile.toFile(), Charset.forName("UTF-8"), CSVFormat.DEFAULT.withFirstRecordAsHeader());
					Map<String, Integer> columnIndices = parsedCsv.getHeaderMap();

					for (CSVRecord csvRecord : parsedCsv.getRecords()) {
						String key = csvRecord.get(columnIndices.get("KEY"));
						if (key == null || key.isEmpty()) {
							continue;
						}
						key = key.toUpperCase();

						String value = csvRecord.get(columnIndices.get(language.getLabelEn().toUpperCase()));

						addValue(language, key, value, keysToLanguagesToValues);
					}

					parsedCsv.close();
				} catch (IOException e) {
					throw new ModLoadingException("Error while processing " + language.getLabelEn(), e);
				}
			}

			modArtifact.setData(keysToLanguagesToValues);
	}

	@Override
	public void combine(List<ModArtifact> artifacts, Path tempDir) throws ModLoadingException, IOException {
		for (ModArtifact artifact : artifacts) {
			Map<String, Map<String, String>> keysToLanguagesToValues = (Map<String, Map<String, String>>) artifact.getData();
			for (Map.Entry<String, Map<String, String>> keyEntry : keysToLanguagesToValues.entrySet()) {
				String key = keyEntry.getKey();
				for (Map.Entry<String, String> languageEntry : keyEntry.getValue().entrySet()) {
					String language = languageEntry.getKey();
					String value = languageEntry.getValue();

					combinedKeysToLanguagesToValues.computeIfAbsent(key, x -> new TreeMap<>()).put(language, value);
				}
			}

		}

	}

	@Override
	public void write(Path assetsDir) throws IOException {
		initLanguages(assetsDir);
		this.outputTo(assetsDir.resolve(definition.getName()));
	}

	private void initLanguages(Path modDir) throws IOException {
		this.languages = objectMapper.readValue(FileUtils.readFileToString(modDir.resolve("translations/languages.json").toFile(), "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, LanguageType.class));

		LanguageType notesColumn = new LanguageType();
		notesColumn.setLabelEn("Notes");
		notesColumn.setLabel("Notes");
		languages.add(1, notesColumn);
	}

	private void addValue(LanguageType language, String key, String value, Map<String, Map<String, String>> keysToLanguagesToValues) {
		if (value == null || value.isEmpty()) {
			return;
		}
		keysToLanguagesToValues.computeIfAbsent(key, k -> new LinkedHashMap<>()).put(language.getLabelEn().toUpperCase(), value);
	}

	public void outputTo(Path targetFile) throws IOException {
		CSVPrinter printer = new CSVPrinter(new FileWriter(targetFile.toFile()), CSVFormat.DEFAULT);

		// Write first line
		printer.print("KEY");
		for (LanguageType language : languages) {
			printer.print(language.getLabelEn().toUpperCase());
		}
		printer.println();

		List<String> allKeys = new ArrayList<>(combinedKeysToLanguagesToValues.keySet());
		Collections.sort(allKeys);


		String lastKey = "";
		for (String key : allKeys) {
			if (!firstPart(key).equals(firstPart(lastKey))) {
				printer.println();
			}
			printer.print(key);

			for (LanguageType language : languages) {
				String value = combinedKeysToLanguagesToValues.get(key).get(language.getLabelEn().toUpperCase());
				if (value == null) {
					value = "";
				}
				printer.print(value);
			}

			printer.println();
			lastKey = key;
		}

		printer.flush();
		IOUtils.closeQuietly(printer);
	}

	private String firstPart(String key) {
		if (key.indexOf('.') > 0) {
			return key.substring(0, key.indexOf('.'));
		} else {
			return key;
		}
	}

}
