package technology.rocketjump.undermount.entities.factories.names;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class NameWordDictionary {

	private Map<String, List<NameWord>> bySphere = new TreeMap<>();

	public NameWordDictionary(File wordsCsv) throws IOException {
		List<String> lines = FileUtils.readLines(wordsCsv);
		for (String line : lines) {
			if (line.length() == 0 || line.split(",").length == 0) {
				continue;
			}
			NameWord nameWord = new NameWord();
			nameWord.readFromLine(line);

			for (String sphere : nameWord.spheres) {
				List<NameWord> wordsForSphere = bySphere.computeIfAbsent(sphere, s -> new ArrayList<>());
				wordsForSphere.add(nameWord);
			}

		}
	}

	public List<NameWord> getBySphere(String sphere) {
		return bySphere.getOrDefault(sphere, Collections.emptyList());
	}

}
