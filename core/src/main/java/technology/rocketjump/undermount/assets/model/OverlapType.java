package technology.rocketjump.undermount.assets.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.undermount.misc.Name;

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

}
