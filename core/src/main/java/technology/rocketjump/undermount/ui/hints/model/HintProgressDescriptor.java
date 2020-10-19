package technology.rocketjump.undermount.ui.hints.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HintProgressDescriptor {

	private ProgressDescriptorTargetType type;
	private String targetTypeName;
	private int quantityRequired;

	public enum ProgressDescriptorTargetType {

		ROOMS,
		ROOM_TILES,
		FARM_PLOT_SELECTIONS,
		PROFESSIONS_ASSIGNED,
		FURNITURE_CONSTRUCTED,
		ITEM_EXISTS

	}

	public ProgressDescriptorTargetType getType() {
		return type;
	}

	public void setType(ProgressDescriptorTargetType type) {
		this.type = type;
	}

	public String getTargetTypeName() {
		return targetTypeName;
	}

	public void setTargetTypeName(String targetTypeName) {
		this.targetTypeName = targetTypeName;
	}

	public int getQuantityRequired() {
		return quantityRequired;
	}

	public void setQuantityRequired(int quantityRequired) {
		this.quantityRequired = quantityRequired;
	}

	@Override
	public String toString() {
		return "HintProgressDescriptor{" +
				"type=" + type +
				", targetTypeName='" + targetTypeName + '\'' +
				", quantityRequired=" + quantityRequired +
				'}';
	}
}
