package technology.rocketjump.undermount.entities.ai.memory;

public enum MemoryType {

	FAILED_GOAL(2.0),
	ATTACKED_BY_CREATURE(0.3),
	LACKING_REQUIRED_ITEM(12.0),
	CONSUMED_ALCOHOLIC_DRINK(20.0),

	ABOUT_TO_HAVE_A_BREAKDOWN(1.0),
	HAD_A_TANTRUM(null);

	public final Double shortTermMemoryDurationHours;

	MemoryType(Double shortTermMemoryDurationHours) {
		this.shortTermMemoryDurationHours = shortTermMemoryDurationHours;
	}
}
