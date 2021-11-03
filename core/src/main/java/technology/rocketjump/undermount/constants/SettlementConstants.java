package technology.rocketjump.undermount.constants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.model.physical.creature.Race;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SettlementConstants {

	private String kitchenProfession;

	private String haulingJobType;
	private String constructRoofingJobType;
	private String deconstructRoofingJobType;
	private String constructPipingJobType;
	private String deconstructPipingJobType;
	private String constructMechanismJobType;
	private String deconstructMechanismJobType;
	private String fishingJobType;
	private List<String> fishAvailable = new ArrayList<>();
	@JsonIgnore
	private List<Race> fishRacesAvailable = new ArrayList<>();

	private double mushroomShockTimeHours;
	private int numAnnualFish;

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

	public String getDeconstructRoofingJobType() {
		return deconstructRoofingJobType;
	}

	public void setDeconstructRoofingJobType(String deconstructRoofingJobType) {
		this.deconstructRoofingJobType = deconstructRoofingJobType;
	}

	public String getConstructPipingJobType() {
		return constructPipingJobType;
	}

	public void setConstructPipingJobType(String constructPipingJobType) {
		this.constructPipingJobType = constructPipingJobType;
	}

	public String getDeconstructPipingJobType() {
		return deconstructPipingJobType;
	}

	public void setDeconstructPipingJobType(String deconstructPipingJobType) {
		this.deconstructPipingJobType = deconstructPipingJobType;
	}

	public String getConstructMechanismJobType() {
		return constructMechanismJobType;
	}

	public void setConstructMechanismJobType(String constructMechanismJobType) {
		this.constructMechanismJobType = constructMechanismJobType;
	}

	public String getDeconstructMechanismJobType() {
		return deconstructMechanismJobType;
	}

	public void setDeconstructMechanismJobType(String deconstructMechanismJobType) {
		this.deconstructMechanismJobType = deconstructMechanismJobType;
	}

	public int getNumAnnualFish() {
		return numAnnualFish;
	}

	public void setNumAnnualFish(int numAnnualFish) {
		this.numAnnualFish = numAnnualFish;
	}

	public String getFishingJobType() {
		return fishingJobType;
	}

	public void setFishingJobType(String fishingJobType) {
		this.fishingJobType = fishingJobType;
	}

	public List<String> getFishAvailable() {
		return fishAvailable;
	}

	public void setFishAvailable(List<String> fishAvailable) {
		this.fishAvailable = fishAvailable;
	}

	public List<Race> getFishRacesAvailable() {
		return fishRacesAvailable;
	}

	public void setFishRacesAvailable(List<Race> fishRacesAvailable) {
		this.fishRacesAvailable = fishRacesAvailable;
	}
}
