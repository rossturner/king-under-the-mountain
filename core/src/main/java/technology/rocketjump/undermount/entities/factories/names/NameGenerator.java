package technology.rocketjump.undermount.entities.factories.names;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Gender;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidName;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public abstract class NameGenerator {

	protected final NameWordDictionary adjectiveDictionary;
	protected final NameWordDictionary nounDictionary;
	protected final NorseNameGenerator norseNameGenerator;

	protected final List<String> goodSpheres = new LinkedList<>();
	protected final List<String> badSpheres = new LinkedList<>();
	private final JSONObject descriptor;

	@Inject
	public NameGenerator(File descriptorFile, NorseNameGenerator norseNameGenerator) throws IOException {
		this.norseNameGenerator = norseNameGenerator;
		adjectiveDictionary = new NameWordDictionary(new File("assets/text/adjective_noun/adjectives.csv"));
		nounDictionary = new NameWordDictionary(new File("assets/text/adjective_noun/nouns.csv"));

		descriptor = JSON.parseObject(FileUtils.readFileToString(descriptorFile));

		JSONArray goodSpheres = descriptor.getJSONArray("goodSpheres");
		for (Object goodSphere : goodSpheres) {
			this.goodSpheres.add((String)goodSphere);
		}

		JSONArray badSpheres = descriptor.getJSONArray("badSpheres");
		for (Object badSphere : badSpheres) {
			this.badSpheres.add((String)badSphere);
		}
	}

	public HumanoidName create(long seed, Gender gender) {
		HumanoidName name = new HumanoidName();
		name.setFirstName(generateUsingScheme(descriptor.getString("firstName"), seed, gender, null));
		name.setLastName(generateUsingScheme(descriptor.getString("familyName"), seed, gender, name.getFirstName().length() > 0 ? name.getFirstName().substring(0, 1) : null));
		return name;
	}

	public String generateUsingScheme(String scheme, long seed, Gender gender, String alliterationMatcher) {
		switch (scheme) {
			case "none":
				return "";
			case "adjective_noun":
				return createAdjectiveNounName(seed, alliterationMatcher);
			case "given_names":
				return norseNameGenerator.createGivenName(seed, gender);
			default:
				Logger.error("Unrecognised name generation scheme: " + scheme);
				return "ERROR";
		}
	}

	protected String createAdjectiveNounName(long seed, String alliterationMatcher) {
		Random random = new RandomXS128(seed);

		String adjective = pickFrom(adjectiveDictionary, random, alliterationMatcher);
		String noun = pickFrom(nounDictionary, random, null);

		String lastOfAdjective = adjective.substring(adjective.length() - 1).toLowerCase();
		String firstOfNoun = noun.substring(0, 1).toLowerCase();
		if (lastOfAdjective.equals(firstOfNoun)) {
			return createAdjectiveNounName(seed + 1, alliterationMatcher);
		}

		String combined = adjective + noun;
		return WordUtils.capitalize(combined.toLowerCase());
	}

	protected String pickFrom(NameWordDictionary adjectiveDictionary, Random random, String alliterationMatcher) {
		List<NameWord> toPickFrom = new ArrayList<>();

		for (String goodSphere : goodSpheres) {
			for (NameWord potentialWord : adjectiveDictionary.getBySphere(goodSphere)) {
				if (!Collections.disjoint(potentialWord.spheres, badSpheres)) {
					continue;
				}
				if (!toPickFrom.contains(potentialWord)) {
					toPickFrom.add(potentialWord);
				}
			}
		}

		if (toPickFrom.isEmpty()) {
			Logger.error("No valid names to pick from in " + this.getClass().getSimpleName());
			return "";
		} else {
			List<String> grabBag = new ArrayList<>();

			for (NameWord nameWord : toPickFrom) {
				int prevalence = nameWord.prevalence;
				if (alliterationMatcher != null && nameWord.word.startsWith(alliterationMatcher)) {
					prevalence *= 3; // Triple chance of word with alliteration (same starting letter)
				}
				for (int chances = 0; chances < prevalence; chances++) {
					grabBag.add(nameWord.word);
				}
			}

			return grabBag.get(random.nextInt(grabBag.size()));
		}
	}
}
