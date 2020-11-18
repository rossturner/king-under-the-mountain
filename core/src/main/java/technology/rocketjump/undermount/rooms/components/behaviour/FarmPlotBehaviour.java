package technology.rocketjump.undermount.rooms.components.behaviour;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesSeed;
import technology.rocketjump.undermount.environment.model.Season;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.JobCreatedCallback;
import technology.rocketjump.undermount.messaging.types.RequestHaulingMessage;
import technology.rocketjump.undermount.messaging.types.RequestPlantRemovalMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.components.FarmPlotComponent;
import technology.rocketjump.undermount.rooms.components.RoomComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage.PlantSpeciesHarvestType.FARMING;
import static technology.rocketjump.undermount.jobs.model.JobState.REMOVED;

public class FarmPlotBehaviour extends RoomBehaviourComponent implements JobCreatedCallback, Prioritisable {

	private Map<GridPoint2, Job> jobsByTiles = new HashMap<>();
	private JobType tillingJobType;
	private JobType plantingJobType;
	private JobType harvestingJobType;

	public FarmPlotBehaviour(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {

	}

	public void setJobTypes(JobType tillingJobType, JobType plantingJobType, JobType harvestingJobType) {
		this.tillingJobType = tillingJobType;
		this.plantingJobType = plantingJobType;
		this.harvestingJobType = harvestingJobType;
	}

	@Override
	public RoomComponent clone(Room newParent) {
		FarmPlotBehaviour cloned = new FarmPlotBehaviour(newParent, messageDispatcher);
		// Duplicate all existing jobs over, they'll be dealt with when completed
		for (Map.Entry<GridPoint2, Job> entry : jobsByTiles.entrySet()) {
			cloned.jobsByTiles.put(entry.getKey(), entry.getValue());
		}
		cloned.tillingJobType = this.tillingJobType;
		cloned.plantingJobType = this.plantingJobType;
		cloned.harvestingJobType = this.harvestingJobType;
		return cloned;
	}

	@Override
	public void mergeFrom(RoomComponent otherComponent) {
		FarmPlotBehaviour other = (FarmPlotBehaviour)otherComponent;
		for (Map.Entry<GridPoint2, Job> entry : other.jobsByTiles.entrySet()) {
			this.jobsByTiles.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
		clearOutOfSeasonPlantingJobs(gameContext, messageDispatcher);
		clearWrongCropPlantingJob();
		clearCompletedJobs();

		boolean allTilesTilled = allTilesTilled(gameContext);

		for (GridPoint2 tileLocation : parent.getRoomTiles().keySet()) {
			if (!jobsByTiles.containsKey(tileLocation)) {
				addAnyRequiredJob(tileLocation, gameContext, messageDispatcher, allTilesTilled);
			}
		}

	}

	@Override
	public void tileRemoved(GridPoint2 location) {
		Job jobAtLocation = jobsByTiles.remove(location);
		if (jobAtLocation != null && messageDispatcher != null) {
			messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, jobAtLocation);
		}
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Job job : jobsByTiles.values()) {
			job.setJobPriority(jobPriority);
		}
	}

	private void addAnyRequiredJob(GridPoint2 tileLocation, GameContext gameContext, MessageDispatcher messageDispatcher,
								   boolean allTilesTilled) {
		MapTile tile = gameContext.getAreaMap().getTile(tileLocation);
		if (tile == null) {
			Logger.error(this.getClass().getSimpleName() + " is processing a null tile");
		} else {
			if (entityToRemoveInTile(tile, messageDispatcher)) {
				return;
			}

			// Turn ground to tilled
			if (tileNeedsTilling(tile, messageDispatcher)) {
				return;
			}

			Season currentSeason = gameContext.getGameClock().getCurrentSeason();
			FarmPlotComponent farmPlotComponent = parent.getComponent(FarmPlotComponent.class);
			if (allTilesTilled) {
				if (farmPlotComponent.getSelectedCrop() != null && farmPlotComponent.getSelectedCrop().getSeed() != null &&
						farmPlotComponent.getSelectedCrop().getSeed().getPlantingSeasons().contains(currentSeason) &&
						!tileContainsCorrectCrop(tile, farmPlotComponent)) {
					if (!jobsByTiles.containsKey(tile.getTilePosition())) {
						plantSeedJob(tile, farmPlotComponent, messageDispatcher);
					}
				} else if (tileContainsCorrectCrop(tile, farmPlotComponent)) {
					harvestCropIfApplicable(tile, messageDispatcher);
				}
			}
		}
	}

	private boolean tileContainsCorrectCrop(MapTile tile, FarmPlotComponent farmPlotComponent) {
		if (farmPlotComponent.getSelectedCrop() == null) {
			// Nothing selected yet so yes it is the correct crop I guess
			return true;
		}

		for (Entity entity : tile.getEntities()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getSpecies().equals(farmPlotComponent.getSelectedCrop())) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean allTilesTilled(GameContext gameContext) {
		FarmPlotComponent farmPlotComponent = parent.getComponent(FarmPlotComponent.class);
		if (farmPlotComponent.getFarmingFloorType() == null || farmPlotComponent.getFarmingFloorMaterial() == null) {
			return true; // This farm type does not define a required floor
		}

		boolean allTilesAreTilled = true;
		for (GridPoint2 location : parent.getRoomTiles().keySet()) {
			MapTile tile = gameContext.getAreaMap().getTile(location);
			if (tile != null) {
				if (!tile.getFloor().getFloorType().equals(farmPlotComponent.getFarmingFloorType())) {
					allTilesAreTilled = false;
					break;
				}
				if (!tile.getFloor().getMaterial().equals(farmPlotComponent.getFarmingFloorMaterial())) {
					allTilesAreTilled = false;
					break;
				}
			}
		}

		return allTilesAreTilled;
	}

	private boolean entityToRemoveInTile(MapTile tile, MessageDispatcher messageDispatcher) {
		FarmPlotComponent farmPlotComponent = parent.getComponent(FarmPlotComponent.class);

		for (Entity entity : tile.getEntities()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
				if (itemAllocationComponent.getNumUnallocated() > 0) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_ITEM_HAULING,
							new RequestHaulingMessage(entity, entity, true, priority, this));
				}
				return true;
			} else if (entity.getType().equals(EntityType.PLANT)) {
				PlantEntityAttributes plantAttributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (farmPlotComponent.getSelectedCrop() == null || !plantAttributes.getSpecies().equals(farmPlotComponent.getSelectedCrop())) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_PLANT_REMOVAL,
							new RequestPlantRemovalMessage(entity, tile.getTilePosition(), priority, this));
					return true;
				}
			}
		}

		return false;
	}

	private boolean tileNeedsTilling(MapTile tile, MessageDispatcher messageDispatcher) {
		FloorType currentFloorType = tile.getFloor().getFloorType();
		FarmPlotComponent farmPlotComponent = parent.getComponent(FarmPlotComponent.class);
		FloorType desiredFloorType = farmPlotComponent.getFarmingFloorType();
		GameMaterial desiredMaterial = farmPlotComponent.getFarmingFloorMaterial();

		if (desiredFloorType != null && desiredMaterial != null && !currentFloorType.equals(desiredFloorType)) {
			Job tillingJob = new Job(tillingJobType);
			tillingJob.setJobPriority(priority);
			tillingJob.setJobLocation(tile.getTilePosition());
			tillingJob.setReplacementFloorType(desiredFloorType);
			tillingJob.setReplacementFloorMaterial(desiredMaterial);

			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, tillingJob);
			jobCreated(tillingJob);
			return true;
		}

		return false;
	}

	private void plantSeedJob(MapTile tile, FarmPlotComponent farmPlotComponent, MessageDispatcher messageDispatcher) {
		Job plantSeedJob = new Job(plantingJobType);
		plantSeedJob.setJobPriority(priority);
		plantSeedJob.setJobLocation(tile.getTilePosition());

		PlantSpeciesSeed seed = farmPlotComponent.getSelectedCrop().getSeed();
		plantSeedJob.setRequiredItemType(seed.getSeedItemType());
		plantSeedJob.setRequiredItemMaterial(seed.getSeedMaterial());

		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, plantSeedJob);
		jobCreated(plantSeedJob);
	}

	private void clearCompletedJobs() {
		Set<GridPoint2> locations = new HashSet<>(jobsByTiles.keySet());
		for (GridPoint2 location : locations) {
			Job jobAtLocation = jobsByTiles.get(location);
			if (jobAtLocation != null) {
				JobState state = jobAtLocation.getJobState();
				if (REMOVED.equals(state)) {
					jobsByTiles.remove(location);
				}
			}
		}
	}

	private void clearOutOfSeasonPlantingJobs(GameContext gameContext, MessageDispatcher messageDispatcher) {
		PlantSpecies selectedCrop = parent.getComponent(FarmPlotComponent.class).getSelectedCrop();
		if (selectedCrop != null && selectedCrop.getSeed() != null) {
			Season currentSeason = gameContext.getGameClock().getCurrentSeason();
			if (!selectedCrop.getSeed().getPlantingSeasons().contains(currentSeason)) {
				// out of planting season
				for (Job job : jobsByTiles.values()) {
					if (!job.getJobState().equals(JobState.REMOVED) && plantingJobType.equals(job.getType())) {
						messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job);
					}
				}
			}
		}
	}

	private void clearWrongCropPlantingJob() {
		Set<GridPoint2> locations = new HashSet<>(jobsByTiles.keySet());
		PlantSpecies selectedCrop = parent.getComponent(FarmPlotComponent.class).getSelectedCrop();


		for (GridPoint2 location : locations) {
			Job jobAtLocation = jobsByTiles.get(location);
			if (jobAtLocation != null && jobAtLocation.getType().equals(plantingJobType)) {
				GameMaterial requiredSeedMaterial = jobAtLocation.getRequiredItemMaterial();
				if (selectedCrop == null || !requiredSeedMaterial.equals(selectedCrop.getSeed().getSeedMaterial())) {
					messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, jobAtLocation);
				}
			}
		}
	}

	private void harvestCropIfApplicable(MapTile tile, MessageDispatcher messageDispatcher) {
		Entity plantInTile = null;
		for (Entity entity : tile.getEntities()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				plantInTile = entity;
				break;
			}
		}
		if (plantInTile != null) {
			PlantEntityAttributes attributes = (PlantEntityAttributes) plantInTile.getPhysicalEntityComponent().getAttributes();
			PlantSpeciesGrowthStage currentGrowthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());
			if (FARMING.equals(currentGrowthStage.getHarvestType())) {
				Job harvestJob = new Job(harvestingJobType);
				harvestJob.setJobPriority(priority);
				harvestJob.setJobLocation(tile.getTilePosition());
				harvestJob.setTargetId(plantInTile.getId());

				messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, harvestJob);
				jobCreated(harvestJob);
			} else {
				// Not a harvest growth stage, maybe an old job to remove?
				Job existingJob = jobsByTiles.get(tile.getTilePosition());
				if (existingJob != null && !existingJob.getJobState().equals(REMOVED)) {
					messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, existingJob);
				}
			}
		}
	}

	@Override
	public void jobCreated(Job job) {
		jobsByTiles.put(job.getJobLocation(), job);
	}

	// Debug only
	public int numOutstandingJobs() {
		return jobsByTiles.size();
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (!jobsByTiles.isEmpty()) {
			JSONArray jobsJson = new JSONArray();
			for (Job job : jobsByTiles.values()) {
				job.writeTo(savedGameStateHolder);
				jobsJson.add(job.getJobId());
			}
			asJson.put("jobs", jobsJson);
		}

		asJson.put("tillingJobType", tillingJobType.getName());
		asJson.put("plantingJobType", plantingJobType.getName());
		asJson.put("harvestingJobType", harvestingJobType.getName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson,savedGameStateHolder, relatedStores);

		JSONArray jobsJson = asJson.getJSONArray("jobs");
		if (jobsJson != null) {
			for (int cursor = 0; cursor < jobsJson.size(); cursor++) {
				Job job = savedGameStateHolder.jobs.get(jobsJson.getLongValue(cursor));
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobsJson.getLongValue(cursor));
				}
				jobsByTiles.put(job.getJobLocation(), job);
			}
		}


		this.tillingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("tillingJobType"));
		if (tillingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("tillingJobType"));
		}
		this.plantingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("plantingJobType"));
		if (plantingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("plantingJobType"));
		}
		this.harvestingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("harvestingJobType"));
		if (harvestingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("harvestingJobType"));
		}
	}
}
