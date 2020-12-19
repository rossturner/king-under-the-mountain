package technology.rocketjump.undermount.assets.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.undermount.misc.Name;

import java.util.Objects;

public class OverlapType {

	@Name
	private final String overlapName;

	@JsonCreator
	public OverlapType(@JsonProperty("overlapName") String overlapName) {
		this.overlapName = overlapName;
	}

	public String getOverlapName() {
		return overlapName;
	}

	@Override
	public String toString() {
		return overlapName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OverlapType that = (OverlapType) o;
		return overlapName.equals(that.overlapName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(overlapName);
	}
}
