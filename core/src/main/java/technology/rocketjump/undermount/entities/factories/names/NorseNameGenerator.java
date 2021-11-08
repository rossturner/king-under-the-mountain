package technology.rocketjump.undermount.entities.factories.names;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.entities.model.physical.creature.HumanoidName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static technology.rocketjump.undermount.entities.model.physical.creature.Gender.MALE;

/**
 * MODDING This should be one of several possible types of names (e.g. different nationalities)
 */
@Singleton
public class NorseNameGenerator {

	private final List<String> maleGivenNames = new ArrayList<>();
	private final List<String> femaleGivenNames = new ArrayList<>();

	private static final String MALE_PATRONYM_SUFFIX = "son";
	private static final String FEMALE_PATRONYM_SUFFIX = "dóttir";

	@Inject
	public NorseNameGenerator() throws IOException {
		this(new File("assets/text/old_norse/given_names.csv"));
	}

	public NorseNameGenerator(File givenNamesFile) throws IOException {
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		try {
			fileReader = new FileReader(givenNamesFile);
			bufferedReader = new BufferedReader(fileReader);

			String line = bufferedReader.readLine();
			while (line != null) {
				String[] splitLine = line.split(",");
				if (splitLine[1].equals("e")) {
					maleGivenNames.add(splitLine[0]);
					femaleGivenNames.add(splitLine[0]);
				} else if (splitLine[1].equals("f")) {
					femaleGivenNames.add(splitLine[0]);
				} else {
					maleGivenNames.add(splitLine[0]);
				}

				line = bufferedReader.readLine();
			}
		} finally {
			IOUtils.closeQuietly(fileReader);
			IOUtils.closeQuietly(bufferedReader);
		}

	}

	public String createGivenName(long seed, Gender gender) {
		Random random = new RandomXS128(seed);
		List<String> givenNameList = maleGivenNames;
		if (gender.equals(Gender.FEMALE)) {
			givenNameList = femaleGivenNames;
		}

		return givenNameList.get(random.nextInt(givenNameList.size()));
	}

	public HumanoidName create(long seed, Gender gender) {
		Random random = new RandomXS128(seed);
		HumanoidName name = new HumanoidName();

		name.setFirstName(createGivenName(seed, gender));

		String lastName = maleGivenNames.get(random.nextInt(maleGivenNames.size()));
		lastName = convertForSurname(lastName);
		if (gender.equals(MALE)) {
			lastName += MALE_PATRONYM_SUFFIX;
		} else {
			lastName += FEMALE_PATRONYM_SUFFIX;
		}
		name.setLastName(lastName);

		return name;
	}

	private static Map<String, String> genitiveReplacements = new LinkedHashMap<>();
	static {
		genitiveReplacements.put("maðr", "manns");
		genitiveReplacements.put("örn", "arnar");
		genitiveReplacements.put("dan", "danar");
		genitiveReplacements.put("mundr", "mundar");
		genitiveReplacements.put("ðr", "ðar");
		genitiveReplacements.put("ðunn", "ðunar");

		genitiveReplacements.put("ir", "is");
		genitiveReplacements.put("r", "s");

		genitiveReplacements.put("ll", "ls");
		genitiveReplacements.put("nn", "ns");
		genitiveReplacements.put("a", "u");
		genitiveReplacements.put("i", "a");
	}

	public String convertForSurname(String lastName) {
		for (Map.Entry<String, String> entry : genitiveReplacements.entrySet()) {
			if (lastName.endsWith(entry.getKey())) {
				return lastName.substring(0, lastName.length() - entry.getKey().length()) + entry.getValue();
			}
		}

		return lastName;
	}
}
