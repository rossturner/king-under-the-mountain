package technology.rocketjump.undermount.entities.ai.memory;

public enum MemoryType {

	FAILED_GOAL(2),
	LACKING_REQUIRED_ITEM(12),
	CONSUMED_ALCOHOLIC_DRINK(20);

	public final double shortTermMemoryDurationHours;

	MemoryType(double shortTermMemoryDurationHours) {
		this.shortTermMemoryDurationHours = shortTermMemoryDurationHours;
	}
}
