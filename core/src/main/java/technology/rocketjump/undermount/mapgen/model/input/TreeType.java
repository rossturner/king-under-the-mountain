package technology.rocketjump.undermount.mapgen.model.input;

import java.util.Objects;

public class TreeType {

	private final String speciesName;
	private float minYPosition = 0;
	private float maxYPosition = 1;

	public TreeType(String speciesName) {
		this.speciesName = speciesName;
	}

	public String getSpeciesName() {
		return speciesName;
	}

	public float getMinYPosition() {
		return minYPosition;
	}

	public void setMinYPosition(float minYPosition) {
		this.minYPosition = minYPosition;
	}

	public float getMaxYPosition() {
		return maxYPosition;
	}

	public void setMaxYPosition(float maxYPosition) {
		this.maxYPosition = maxYPosition;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TreeType treeType = (TreeType) o;
		return Objects.equals(speciesName, treeType.speciesName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(speciesName);
	}
}
