package technology.rocketjump.undermount.entities.factories.names;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class NameWordDictionaryTest {

	public static void main(String... args) throws IOException {
		// Example of words
		for (String filename : Arrays.asList("core/assets/text/adjective_noun/adjectives.csv", "core/assets/text/adjective_noun/nouns.csv")) {
			showFilenameExample(filename);
		}

	}

	private static void showFilenameExample(String filename) throws IOException {
		File inputFile = new File(filename);
		if (!inputFile.exists()) {
			throw new IOException("Could not find file " + inputFile.getAbsolutePath().toString());
		}

		NameWordDictionary nameWordDictionary = new NameWordDictionary(inputFile);

		System.out.println(inputFile.getName() + ":");
		for (String sphere : nameWordDictionary.getAllSpheres()) {
			System.out.print(sphere + "\t\t");
			if (sphere.length() < 7) {
				System.out.print("\t");
			}

			List<NameWord> wordsForSphere = nameWordDictionary.getBySphere(sphere);
			for (NameWord word : wordsForSphere) {
				System.out.print(word.word + "\t");
			}
			System.out.println();
		}

	}

}