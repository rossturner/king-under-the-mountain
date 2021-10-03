package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Lists;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.ai.goap.actions.EntityCreatedCallback;
import technology.rocketjump.undermount.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.undermount.entities.components.furniture.HarvestableEntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobState;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.PlantCreationRequestMessage;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.ui.i18n.I18nString;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.undermount.entities.behaviour.furniture.MushroomLogBehaviour.MushroomLogState.*;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.ui.i18n.I18nTranslator.oneDecimalFormat;

public class MushroomLogBehaviour extends FurnitureBehaviour implements Destructible, SelectableDescription, EntityCreatedCallback, Prioritisable {

	private static final int MUSHROOMS_SPAWNED_AT_A_TIME = 2;
	private static final int MAX_MUSHROOMS_TO_SPAWN = 10;
	private double totalTimeToSpawnMushroom;

	private double lastUpdateGameTime = 0;
	private double currentMushroomSpawnTime = 0;
	private int totalMushroomsSpawned = 0;
	private MushroomLogState state = SPAWNING;
	private JobType harvestJobType;
	private Job harvestJob;
	private GameContext gameContext;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		totalTimeToSpawnMushroom = gameContext.getGameClock().HOURS_IN_DAY * 3.2;
		this.gameContext = gameContext;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (harvestJob != null && !harvestJob.getJobState().equals(JobState.REMOVED)) {
			messageDispatcher.dispatchMessage(MessageType.JOB_CANCELLED, harvestJob);
		}
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		if (harvestJob != null) {
			harvestJob.setJobPriority(jobPriority);
		}
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		if (state.descriptionI18nKey == null) {
			return emptyList();
		}
		Map<String, I18nString> replacements = new HashMap<>();
		float progress = 100f * progress();
		replacements.put("progress", new I18nWord("progress", oneDecimalFormat.format(progress)));
		I18nWord descriptionWord = i18nTranslator.getDictionary().getWord(state.descriptionI18nKey);
		return Lists.newArrayList(i18nTranslator.applyReplacements(descriptionWord, replacements, Gender.ANY));
	}

	private float progress() {
		return Math.min((float) (currentMushroomSpawnTime / totalTimeToSpawnMushroom), 1f);
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		double elapsedTime = gameContext.getGameClock().getCurrentGameTime() - lastUpdateGameTime;
		currentMushroomSpawnTime += elapsedTime;

		if (parentEntity.isOnFire()) {
			if (harvestJob != null) {
				messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, harvestJob);
				harvestJob = null;
			}
			return;
		}

		switch (state) {
			case SPAWNING: {
				if (currentMushroomSpawnTime > totalTimeToSpawnMushroom / 2) {
					addMushrooms(gameContext);
					totalMushroomsSpawned += MUSHROOMS_SPAWNED_AT_A_TIME;
					state = SPAWNED;
				}
				break;
			}
			case SPAWNED: {
				if (currentMushroomSpawnTime >= totalTimeToSpawnMushroom) {
					createHarvestJob(gameContext);
					state = HARVESTABLE;
				}
				break;
			}
			case HARVESTABLE: {
				if (harvested()) {
					harvestJob = null;
					if (totalMushroomsSpawned >= MAX_MUSHROOMS_TO_SPAWN) {
						state = EXHAUSTED;
						messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, parentEntity);
					} else {
						currentMushroomSpawnTime = 0;
						state = SPAWNING;
					}
				}
				break;
			}
			case EXHAUSTED:
				// do nothing
		}

		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	private void createHarvestJob(GameContext gameContext) {
		harvestJob = new Job(harvestJobType);
		harvestJob.setJobPriority(priority);
		FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
		if (navigableWorkspace != null) {
			harvestJob.setJobLocation(navigableWorkspace.getAccessedFrom());
		} else {
			harvestJob.setJobLocation(toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition()));
		}
		harvestJob.setTargetId(parentEntity.getId());

		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, harvestJob);
	}

	private void addMushrooms(GameContext gameContext) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		GameMaterial primaryMaterial = attributes.getMaterials().get(attributes.getPrimaryMaterialType());
		for (int counter = 0; counter < MUSHROOMS_SPAWNED_AT_A_TIME; counter++) {
			messageDispatcher.dispatchMessage(MessageType.PLANT_CREATION_REQUEST, new PlantCreationRequestMessage(
				primaryMaterial, this
			));
		}

		HarvestableEntityComponent harvestableEntityComponent = parentEntity.getOrCreateComponent(HarvestableEntityComponent.class);
		harvestableEntityComponent.clear();

		DecorationInventoryComponent decorationInventoryComponent = parentEntity.getComponent(DecorationInventoryComponent.class);
		if (decorationInventoryComponent != null) {
			for (Entity entity : decorationInventoryComponent.getDecorationEntities()) {
				PlantEntityAttributes plantAttributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				PlantSpeciesGrowthStage growthStage = plantAttributes.getSpecies().getGrowthStages().get(plantAttributes.getGrowthStageCursor());
				if (!growthStage.getHarvestedItems().isEmpty()) {
					// Only add first item (to avoid seeds/mushroom spawn)
					harvestableEntityComponent.add(growthStage.getHarvestedItems().get(0));
				}
			}
		}

		if (harvestableEntityComponent.isEmpty()) {
			Logger.error("Could not set up any harvestable items in " + this.getClass().getSimpleName());
		}
	}

	@Override
	public void entityCreated(Entity entity) {
		if (entity != null) {
			messageDispatcher.dispatchMessage(MessageType.ENTITY_DO_NOT_TRACK, entity);

			DecorationInventoryComponent decorationInventoryComponent = parentEntity.getComponent(DecorationInventoryComponent.class);
			if (decorationInventoryComponent == null) {
				decorationInventoryComponent = new DecorationInventoryComponent();
				decorationInventoryComponent.init(parentEntity, messageDispatcher, gameContext);
				parentEntity.addComponent(decorationInventoryComponent);
			}
			decorationInventoryComponent.add(entity);
		}
	}

	private boolean harvested() {
		return parentEntity.getOrCreateComponent(DecorationInventoryComponent.class).getDecorationEntities().isEmpty();
	}

	public void setHarvestJobType(JobType harvestJobType) {
		this.harvestJobType = harvestJobType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
		asJson.put("lastUpdate", lastUpdateGameTime);
		asJson.put("currentSpawnTime", currentMushroomSpawnTime);
		if (totalMushroomsSpawned != 0) {
			asJson.put("totalSpawned", totalMushroomsSpawned);
		}
		if (!state.equals(SPAWNING)) {
			asJson.put("state", state);
		}
		asJson.put("harvestJobType", harvestJobType.getName());
		if (harvestJob != null) {
			harvestJob.writeTo(savedGameStateHolder);
			asJson.put("harvestJob", harvestJob.getJobId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
		this.lastUpdateGameTime = asJson.getDoubleValue("lastUpdate");
		this.currentMushroomSpawnTime = asJson.getDoubleValue("currentSpawnTime");
		this.totalMushroomsSpawned = asJson.getIntValue("totalSpawned");
		this.state = EnumParser.getEnumValue(asJson, "state", MushroomLogState.class, SPAWNING);

		Long harvestJobId = asJson.getLong("harvestJob");
		if (harvestJobId != null) {
			harvestJob = savedGameStateHolder.jobs.get(harvestJobId);
			if (harvestJob == null) {
				throw new InvalidSaveException("Could not find job for " + this.getClass().getSimpleName());
			}
		}

		harvestJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("harvestJobType"));
		if (harvestJobType == null) {
			throw new InvalidSaveException("Could not find job type " + asJson.getString("harvestJobType") + " in " + this.getClass().getSimpleName());
		}
	}

	public enum MushroomLogState {

		SPAWNING("CROP.HARVEST_PROGRESS"),
		SPAWNED("CROP.HARVEST_PROGRESS"),
		HARVESTABLE("CROP.HARVEST_PROGRESS"),
		EXHAUSTED(null);

		public final String descriptionI18nKey;

		MushroomLogState(String descriptionI18nKey) {
			this.descriptionI18nKey = descriptionI18nKey;
		}
	}

}
