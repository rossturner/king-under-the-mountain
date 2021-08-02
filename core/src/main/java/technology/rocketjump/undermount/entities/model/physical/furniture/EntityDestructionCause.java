package technology.rocketjump.undermount.entities.model.physical.furniture;

public enum EntityDestructionCause {

	BURNED("ENTITY.DESTRUCTION_DESCRIPTION.BURNED"),
	OXIDISED("ENTITY.DESTRUCTION_DESCRIPTION.OXIDISED"),
	TANTRUM("ENTITY.DESTRUCTION_DESCRIPTION.TANTRUM");

	public final String i18nKey;

	EntityDestructionCause(String i18nKey) {
		this.i18nKey = i18nKey;
	}
}
