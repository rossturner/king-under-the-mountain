package technology.rocketjump.undermount.entities.model.physical.humanoid;

public enum Race {

	DWARF("RACE.DWARF"),
	HUMAN("RACE.HUMAN"), // Note these other races or i18nKeys aren't used yet
	ANY("RACE.ANY");

	public final String i18nKey;

	Race(String i18nKey) {
		this.i18nKey = i18nKey;
	}
}
