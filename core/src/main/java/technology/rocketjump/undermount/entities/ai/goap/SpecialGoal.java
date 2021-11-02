package technology.rocketjump.undermount.entities.ai.goap;

public enum SpecialGoal {

	IDLE("Idle goal"),
	HAUL_ITEM("Haul item goal"),
	DUMP_ITEM("Dump item goal"),
	TRANSFER_LIQUID("Transfer liquid goal"),
	MOVE_LIQUID_IN_ITEM("Move liquid in item goal"),
	REMOVE_LIQUID("Remove liquid goal"),
	DUMP_LIQUID("Dump liquid goal"),
	PLACE_ITEM("Place item goal"),
	ROLL_ON_FLOOR("Roll on floor goal"),
	DOUSE_SELF("Douse self goal"),
	EXTINGUISH_FIRE("Extinguish fire goal"),
	HUNT_CREATURE("Hunt creature goal"),
	ABANDON_JOB("Abandon job goal"),
	ATTACK_AGGRESSOR("Attack aggressor goal"),
	FLEE_FROM_AGGRESSOR("Flee from aggressor goal");

	public final String goalName;
	Goal goalInstance;

	SpecialGoal(String goalName) {
		this.goalName = goalName;
	}

	public Goal getInstance() {
		return goalInstance;
	}
}
