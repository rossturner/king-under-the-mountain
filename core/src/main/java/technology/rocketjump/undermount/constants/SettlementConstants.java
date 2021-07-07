package technology.rocketjump.undermount.constants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SettlementConstants {

	private String kitchenProfession;

	private String haulingJobType;
	private String constructRoofingJobType;

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

	public String getConstructRoofingJobType() {
		return constructRoofingJobType;
	}

	public void setConstructRoofingJobType(String constructRoofingJobType) {
		this.constructRoofingJobType = constructRoofingJobType;
	}

	public double getMushroomShockTimeHours() {
		return mushroomShockTimeHours;
	}

	public void setMushroomShockTimeHours(double mushroomShockTimeHours) {
		this.mushroomShockTimeHours = mushroomShockTimeHours;
	}
}
