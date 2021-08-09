package technology.rocketjump.undermount.entities.behaviour.plants;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.plant.*;
import technology.rocketjump.undermount.environment.model.Season;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileNeighbours;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.EntityMessage;
import technology.rocketjump.undermount.messaging.types.PlantSeedDispersedMessage;
import technology.rocketjump.undermount.messaging.types.RemoveDesignationMessage;
import technology.rocketjump.undermount.messaging.types.ShedLeavesMessage;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType.CROP;
import static technology.rocketjump.undermount.materials.model.GameMaterialType.EARTH;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class PlantBehaviour implements BehaviourComponent {

	public static final float MIN_SUNLIGHT_FOR_GROWING = 0.8f;
	private static final double SUNLIGHT_MULTIPLIER = 1.5;
	private static final float CHANCE_OF_PEST_PER_DAY = 1f / 5f;
	private static final double PEST_GROWTH_SPEED_MULTIPLIER = 0.6;

	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private JobType removePestsJobType;

	private Double lastUpdateGameTime;

	// TODO Should the below be moved to plant attributes?
	private double gameSeasonsToNoticeSeasonChange;
	private Season seasonPlantThinksItIs;
	private PlantSeasonSettings currentSeasonSettings;
	private int lastUpdateDayNumber = 0;
	private Double gameTimeToApplyPests = null;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;

		if (seasonPlantThinksItIs == null) {
			// This is a fresh instance
			resetSeasonChangeTimer(gameContext);
		} else {
			// This instance is loaded from a save
			PlantEntityAttributes attributes = (PlantEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
			currentSeasonSettings = attributes.getSpecies().getSeasons().get(seasonPlantThinksItIs);
		}
	}

	private void resetSeasonChangeTimer(GameContext gameContext) {
		PlantEntityAttributes attributes = (PlantEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		this.gameSeasonsToNoticeSeasonChange = 0.1 * (1 / attributes.getGrowthRate());
		this.seasonPlantThinksItIs = gameContext.getGameClock().getCurrentSeason();
		this.currentSeasonSettings = attributes.getSpecies().getSeasons().get(seasonPlantThinksItIs);
		attributes.setSeasonProgress(0);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// Do nothing, does not update every frame
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {

		if (lastUpdateGameTime == null) {
			lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		}

		PlantEntityAttributes attributes = (PlantEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		PlantSpecies species = attributes.getSpecies();
		MapTile parentEntityTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition());

		if (!EARTH.equals(parentEntityTile.getFloor().getFloorType().getMaterialType())) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, new EntityMessage(this.parentEntity.getId()));
			return;
		}

		if (species.getPlantType().equals(PlantSpeciesType.SHRUB)) {
			if (checkForCompetition(gameContext)) {
				messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, new EntityMessage(this.parentEntity.getId()));
				return;
			}
		}


		PlantSpeciesGrowthStage growthStage = species.getGrowthStages().get(attributes.getGrowthStageCursor());
		Season currentSeason = gameContext.getGameClock().getCurrentSeason();

		double currentGameTime = gameContext.getGameClock().getCurrentGameTime();
		double elapsedGameSeasons = gameContext.getGameClock().gameHoursAsNumSeasons(currentGameTime - lastUpdateGameTime);
		elapsedGameSeasons = elapsedGameSeasons * (double) attributes.getGrowthRate();
		double elapsedTime = currentGameTime - lastUpdateGameTime;
		lastUpdateGameTime = currentGameTime;

		boolean dayElapsed = false;
		if (lastUpdateDayNumber != gameContext.getGameClock().getCurrentDayNumber()) {
			// Day elapsed
			dayElapsed = true;
			lastUpdateDayNumber = gameContext.getGameClock().getCurrentDayNumber();
		}

		attributes.setSeasonProgress(attributes.getSeasonProgress() + elapsedGameSeasons);


		if (!currentSeason.equals(seasonPlantThinksItIs)) {
			gameSeasonsToNoticeSeasonChange -= elapsedGameSeasons;
			if (gameSeasonsToNoticeSeasonChange <= 0) {
				resetSeasonChangeTimer(gameContext);
				if (currentSeasonSettings != null && currentSeasonSettings.getSwitchToGrowthStage() != null) {
					PlantSpeciesGrowthStage nextGrowthStage = species.getGrowthStages().get(currentSeasonSettings.getSwitchToGrowthStage());
					checkToRemoveForaging(growthStage, nextGrowthStage, parentEntityTile);
					attributes.setGrowthStageCursor(currentSeasonSettings.getSwitchToGrowthStage());
					attributes.setGrowthStageProgress(0);
					growthStage = nextGrowthStage;
				}
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity); // Might need to now show fruit
			}
		}


		float currentGrowth = attributes.getGrowthStageProgress();
		if (currentSeasonSettings == null || currentSeasonSettings.isGrowth()) {
			if (gameContext.getMapEnvironment().getSunlightAmount() > MIN_SUNLIGHT_FOR_GROWING) {
				// Multiplying growth to balance out nighttime
				elapsedGameSeasons *= SUNLIGHT_MULTIPLIER;
				float extraGrowth = (float)(elapsedGameSeasons / growthStage.getSeasonsUntilComplete());
				if (attributes.isAfflictedByPests()) {
					extraGrowth *= PEST_GROWTH_SPEED_MULTIPLIER;
				}
				currentGrowth += extraGrowth;
			}


			// Should pests apply today? Only check when growing
			if (dayElapsed && attributes.getSpecies().getPlantType().equals(CROP)) {
				boolean applyPestsToday = gameContext.getRandom().nextFloat() < CHANCE_OF_PEST_PER_DAY;
				if (!attributes.isAfflictedByPests() && applyPestsToday) {
					gameTimeToApplyPests = gameContext.getGameClock().getCurrentGameTime() + (gameContext.getRandom().nextFloat() * gameContext.getGameClock().HOURS_IN_DAY);
				}
			}
		}

		if (gameTimeToApplyPests != null && gameTimeToApplyPests < gameContext.getGameClock().getCurrentGameTime()) {
			Job removePestsJob = new Job(removePestsJobType);
			// try to set priority based on room (farm plot) we are in
			if (parentEntityTile.hasRoom() && parentEntityTile.getRoomTile().getRoom().getBehaviourComponent() instanceof Prioritisable) {
				removePestsJob.setJobPriority(((Prioritisable)parentEntityTile.getRoomTile().getRoom().getBehaviourComponent()).getPriority());
			}
			removePestsJob.setTargetId(parentEntity.getId());
			removePestsJob.setJobLocation(toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, removePestsJob);

			gameTimeToApplyPests = null;
			attributes.setAfflictedByPests(removePestsJob);
		}

		if (currentGrowth >= 1) {
			currentGrowth = 0;
			growthStageComplete(parentEntityTile);
		}
		attributes.setGrowthStageProgress(currentGrowth);
		boolean assetUpdateRequired = attributes.updateColors(seasonPlantThinksItIs);
		if (assetUpdateRequired) {
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
		}

		if (currentSeasonSettings != null && currentSeasonSettings.isShedsLeaves()) {
			messageDispatcher.dispatchMessage(MessageType.TREE_SHED_LEAVES, new ShedLeavesMessage(parentEntity, attributes.getColor(ColoringLayer.LEAF_COLOR)));
		}
	}

	private void growthStageComplete(MapTile parentEntityTile) {
		PlantEntityAttributes attributes = (PlantEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		PlantSpecies species = attributes.getSpecies();
		PlantSpeciesGrowthStage growthStage = species.getGrowthStages().get(attributes.getGrowthStageCursor());

		Integer nextStageCursor = growthStage.getNextGrowthStage();
		if (nextStageCursor == null) {
			nextStageCursor = attributes.getGrowthStageCursor();
		}
		PlantSpeciesGrowthStage nextGrowthStage = species.getGrowthStages().get(nextStageCursor);

		checkToRemoveForaging(growthStage, nextGrowthStage, parentEntityTile);

		for (PlantSpeciesGrowthStage.PlantGrowthCompleteTag onCompletion : growthStage.getOnCompletion()) {
			switch (onCompletion) {
				case DISPERSE_SEEDS:
					messageDispatcher.dispatchMessage(MessageType.PLANT_SEED_DISPERSED,
							new PlantSeedDispersedMessage(species, parentEntity.getLocationComponent().getWorldPosition(), growthStage.isShowFruit()));
					break;
				case DESTROY_PLANT:
					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, new EntityMessage(parentEntity.getId()));
					break;
				default:
					Logger.error("Not yet implemented: Handling of " + onCompletion);

			}
		}

		// FIXME Should check if tileHeight changes between stages, and if so verify that this is able to gain the new height
		/*

	private boolean canIncreaseTileHeight(GameContext gameContext) {
		PlantEntityAttributes attributes = (PlantEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		int nextHeightOffset = attributes.getCurrentTileHeight();
		MapTile targetTile = gameContext.getAreaMap().getTile(toGridPoint(parentEntity.getLocationComponent().getCursorWorldPosition()).add(0, nextHeightOffset));

		if (targetTile == null || targetTile.hasWall() || targetTile.hasTree()) { // TODO might not check for tree
			return false;
		} else {
			return true;
		}
	}
		 */

		attributes.setGrowthStageCursor(nextStageCursor);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
	}

	/**
	 * This method is to see if there are too many neighbouring shrubs or trees
	 *
	 * Returns true if there is too much
	 */
	private boolean checkForCompetition(GameContext gameContext) {
		Vector2 worldPosition = parentEntity.getLocationComponent().getWorldPosition();
		GridPoint2 tilePosition = new GridPoint2((int) Math.floor(worldPosition.x), (int) Math.floor(worldPosition.y));

		TileNeighbours tileNeighbours = gameContext.getAreaMap().getNeighbours(tilePosition.x, tilePosition.y);
		int numNeighbouring = 0;
		for (MapTile neighbourTile : tileNeighbours.values()) {
			if (neighbourTile.hasPlant()) {
				numNeighbouring++;
			}
		}
		return numNeighbouring > gameContext.getConstantsRepo().getWorldConstants().getMaxNeighbouringShrubs();
	}

	private void checkToRemoveForaging(PlantSpeciesGrowthStage currentGrowthStage, PlantSpeciesGrowthStage nextGrowthStage, MapTile parentEntityTile) {
		if (currentGrowthStage.getHarvestType() != null && nextGrowthStage.getHarvestType() == null) {
			if (parentEntityTile.getDesignation() != null && parentEntityTile.getDesignation().getDesignationName().equals("HARVEST")) {
				messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(parentEntityTile, parentEntityTile.getDesignation()));
			}
		}
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		PlantBehaviour cloned = new PlantBehaviour();
		cloned.removePestsJobType = this.removePestsJobType;
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	public Season getSeasonPlantThinksItIs() {
		return seasonPlantThinksItIs;
	}

	public double getGameSeasonsToNoticeSeasonChange() {
		return gameSeasonsToNoticeSeasonChange;
	}

	public void setRemovePestsJobType(JobType removePestsJobType) {
		this.removePestsJobType = removePestsJobType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("removePestsJobType", removePestsJobType.getName());
		asJson.put("lastUpdateGameTime", lastUpdateGameTime);
		asJson.put("gameSeasonsToNoticeSeasonChange", gameSeasonsToNoticeSeasonChange);
		asJson.put("season", seasonPlantThinksItIs.name());
		if (lastUpdateDayNumber != 0) {
			asJson.put("lastUpdateDayNumber", lastUpdateDayNumber);
		}
		if (gameTimeToApplyPests != null) {
			asJson.put("gameTimeToApplyPests", gameTimeToApplyPests);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.removePestsJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("removePestsJobType"));
		if (removePestsJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("removePestsJobType"));
		}
		this.lastUpdateGameTime = asJson.getDouble("lastUpdateGameTime");
		this.gameSeasonsToNoticeSeasonChange = asJson.getDoubleValue("gameSeasonsToNoticeSeasonChange");
		this.seasonPlantThinksItIs = EnumParser.getEnumValue(asJson, "season", Season.class, null);
		this.lastUpdateDayNumber = asJson.getIntValue("lastUpdateDayNumber");
		this.gameTimeToApplyPests = asJson.getDouble("gameTimeToApplyPests");
	}
}
