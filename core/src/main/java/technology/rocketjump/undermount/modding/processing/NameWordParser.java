package technology.rocketjump.undermount.modding.processing;

import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.entities.factories.names.NameWord;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NameWordParser {

	public static List<NameWord> parseNameWords(File csvFileHandle) throws IOException {
		List<String> lines = FileUtils.readLines(csvFileHandle);
		List<NameWord> parsed = new ArrayList<>();

		for (String csvLine : lines) {
			if (csvLine.length() == 0 || csvLine.startsWith(",")) {
				continue;
			}
			NameWord nameWord = new NameWord();
			nameWord.readFromLine(csvLine);
			parsed.add(nameWord);
		}

		return parsed;
	}

	public static void verify(List<NameWord> nameAdjectives, List<NameWord> nameNouns) {
		Map<String, Integer> sphereCounts = new TreeMap<>();
		Set<String> allSpheres = new LinkedHashSet<>();

		checkForDuplicates(nameAdjectives, nameNouns);

		for (NameWord word : nameAdjectives) {
			countSpheres(word, allSpheres, sphereCounts);
		}
		for (NameWord word : nameNouns) {
			countSpheres(word, allSpheres, sphereCounts);
		}

		List<String> sortedSpheres = new ArrayList<>(allSpheres);
		Collections.sort(sortedSpheres);

		System.out.println("Word spheres:");
		for (String sphere : sortedSpheres) {
			System.out.print(sphere + " " + sphereCounts.get(sphere) + "\t");
		}

	}

	private static void checkForDuplicates(List<NameWord> nameAdjectives, List<NameWord> nameNouns) {
		Set<String> uniqueWords = new HashSet<>();

		for (NameWord nameAdjective : nameAdjectives) {
			if (uniqueWords.contains(nameAdjective.word)) {
				throw new RuntimeException("Duplicate word found in adjectives: " + nameAdjective.word);
			} else {
				uniqueWords.add(nameAdjective.word);
			}
		}
		for (NameWord nameNoun : nameNouns) {
			if (uniqueWords.contains(nameNoun.word)) {
				throw new RuntimeException("Duplicate word found in nouns: " + nameNoun.word);
			} else {
				uniqueWords.add(nameNoun.word);
			}
		}


	}

	private static void countSpheres(NameWord word, Set<String> allSpheres, Map<String, Integer> sphereCounts) {
		allSpheres.addAll(word.spheres);

		for (String sphere : word.spheres) {
			if (sphereCounts.containsKey(sphere)) {
				int currentValue = sphereCounts.get(sphere);
				sphereCounts.put(sphere, currentValue + 1);
			} else {
				sphereCounts.put(sphere, 1);
			}
		}
	}
}
