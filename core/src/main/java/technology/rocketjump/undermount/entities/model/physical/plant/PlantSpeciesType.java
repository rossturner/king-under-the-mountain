package technology.rocketjump.undermount.entities.model.physical.plant;

import technology.rocketjump.undermount.jobs.model.JobType;

public enum PlantSpeciesType {

	TREE("LOGGING"),
	SHRUB("CLEAR_GROUND"),
	MUSHROOM_TREE("LOGGING"),
	MUSHROOM("CLEAR_GROUND"),
	CROP("CLEAR_GROUND");


	public final String removalJobTypeName;
	private JobType removalJobType;

	PlantSpeciesType(String removalJobTypeName) {
		this.removalJobTypeName = removalJobTypeName;
	}

	public JobType getRemovalJobType() {
		return removalJobType;
	}

	public void setRemovalJobType(JobType removalJobType) {
		this.removalJobType = removalJobType;
	}
}
