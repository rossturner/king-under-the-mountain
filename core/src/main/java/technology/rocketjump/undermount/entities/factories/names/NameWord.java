package technology.rocketjump.undermount.entities.factories.names;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.undermount.misc.Name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NameWord implements Comparable<NameWord>, LoadedFromCsv {

	@Name
	public String word;
	public int prevalence;
	public final List<String> spheres = new ArrayList<>(3);

	@Override
	public void readFromLine(String csvLine) {
		String[] tokens = csvLine.split(",");
		if (tokens.length == 0) {
			return;
		}
		word = WordUtils.capitalize(tokens[0]);

		if (tokens.length <= 1 || tokens[1].isEmpty()) {
			prevalence = 1;
		} else {
			prevalence = Integer.valueOf(tokens[1]);
		}

		for (int cursor = 2; cursor < tokens.length; cursor++) {
			String sphere = tokens[cursor];
			if (sphere.length() > 0) {
				spheres.add(WordUtils.capitalize(sphere));
			}
		}

		if (spheres.isEmpty()) {
			spheres.add("Unspecified");
		}

		Collections.sort(spheres);
	}

	@Override
	public String writeToLine() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(this.word).append(",").append(this.prevalence);
		for (String sphere : this.spheres) {
			stringBuilder.append(",").append(sphere);
		}
		return stringBuilder.toString();
	}

	@Override
	public String getUniqueName() {
		return word;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NameWord nameWord = (NameWord) o;
		return Objects.equals(word, nameWord.word);
	}

	@Override
	public int hashCode() {
		return Objects.hash(word);
	}

	@Override
	public int compareTo(NameWord o) {
		return this.word.compareTo(o.word);
	}

	@Override
	public String toString() {
		return word + " [" + StringUtils.join(spheres, ", ") + "]";
	}

}
