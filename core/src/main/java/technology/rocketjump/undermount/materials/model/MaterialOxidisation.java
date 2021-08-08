package technology.rocketjump.undermount.materials.model;

public class MaterialOxidisation {

	private OxidisationEffect effect;
	private String changesTo;
	private double hoursToConvert;

	public enum OxidisationEffect {

		CONVERT_MATERIAL,
		DESTROY_PARENT

	}

	public OxidisationEffect getEffect() {
		return effect;
	}

	public void setEffect(OxidisationEffect effect) {
		this.effect = effect;
	}

	public String getChangesTo() {
		return changesTo;
	}

	public void setChangesTo(String changesTo) {
		this.changesTo = changesTo;
	}

	public double getHoursToConvert() {
		return hoursToConvert;
	}

	public void setHoursToConvert(double hoursToConvert) {
		this.hoursToConvert = hoursToConvert;
	}
}
