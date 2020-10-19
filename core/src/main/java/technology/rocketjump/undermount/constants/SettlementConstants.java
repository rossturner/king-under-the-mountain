package technology.rocketjump.undermount.constants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SettlementConstants {

	private String kitchenProfession;

	private String haulingJobType;

	private double mushroomShockTimeHours;

	public String getKitchenProfession() {
		return kitchenProfession;
	}

	public void setKitchenProfession(String kitchenProfession) {
		this.kitchenProfession = kitchenProfession;
	}

	public String getHaulingJobType() {
		return haulingJobType;
	}

	public void setHaulingJobType(String haulingJobType) {
		this.haulingJobType = haulingJobType;
	}

	public double getMushroomShockTimeHours() {
		return mushroomShockTimeHours;
	}

	public void setMushroomShockTimeHours(double mushroomShockTimeHours) {
		this.mushroomShockTimeHours = mushroomShockTimeHours;
	}
}
