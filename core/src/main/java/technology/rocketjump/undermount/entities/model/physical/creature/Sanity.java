package technology.rocketjump.undermount.entities.model.physical.creature;

public enum Sanity {

	SANE(null),
	BROKEN("MADNESS.BROKEN");

	public final String i18nKey;

	Sanity(String i18nKey) {
		this.i18nKey = i18nKey;
	}
}
