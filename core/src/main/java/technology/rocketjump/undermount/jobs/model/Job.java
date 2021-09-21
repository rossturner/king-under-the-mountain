package technology.rocketjump.undermount.jobs.model;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.cooking.model.CookingRecipe;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.components.LiquidAllocation;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rooms.HaulingAllocation;

import java.util.Objects;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

public class Job implements Persistable {

	private static final float TIME_TO_COMPLETE_JOB_WHEN_UNSPECIFIED = 3f;
	private long jobId;
	private JobType type;

	private JobState jobState;
	private JobPriority jobPriority = JobPriority.NORMAL;

	private Long targetId;

	private HaulingAllocation haulingAllocation;
	private LiquidAllocation liquidAllocation;

	private Long assignedToEntityId;

	private float workDoneSoFar;
	private Float overrideWorkDuration;

	private GridPoint2 jobLocation;
	private GridPoint2 secondaryLocation; // Used in crafting where jobLocation is position to stand, secondary is workspace

	private Profession requiredProfession;
	private ItemType requiredItemType;
	private GameMaterial requiredItemMaterial;

	private FloorType replacementFloorType;
	private GameMaterial replacementFloorMaterial;

	private CraftingRecipe craftingRecipe;
	private CookingRecipe cookingRecipe;

	public Job() {

	}

	public Job(JobType type) {
		this(type, SequentialIdGenerator.nextId());
	}

	public Job(JobType type, long jobId) {
		this.jobId = jobId;
		this.type = type;
		this.jobState = JobState.POTENTIALLY_ACCESSIBLE;
		this.requiredProfession = type.getRequiredProfession();
		this.requiredItemType = type.getRequiredItemType();
	}

	public JobTarget getTargetOfJob(GameContext gameContext) {
		if (cookingRecipe != null) {
			return new JobTarget(cookingRecipe);
		}

		if (craftingRecipe != null) {
			return new JobTarget(craftingRecipe, gameContext.getEntities().get(targetId));
		}

		if (targetId != null) {
			Entity targetEntity = gameContext.getEntities().get(targetId);
			if (targetEntity != null) {
				return new JobTarget(targetEntity);
			}
		}

		if (jobLocation != null) {
			MapTile targetTile = gameContext.getAreaMap().getTile(jobLocation);
			if (targetTile.hasConstruction()) {
				return new JobTarget(targetTile.getConstruction());
			} else if (targetTile.hasDoorway()) {
				return new JobTarget(targetTile.getDoorway().getDoorEntity());
			} else if (targetTile.getFloor().hasBridge()) {
				return new JobTarget(targetTile.getFloor().getBridge());
			} else {
				for (Entity targetTileEntity : targetTile.getEntities()) {
					if (targetTileEntity.getType().equals(EntityType.PLANT)) {
						return new JobTarget(targetTileEntity);
					}
				}
				return new JobTarget(targetTile);
			}

		}

		return null;
	}

	public long getJobId() {
		return jobId;
	}

	public JobType getType() {
		return type;
	}

	public JobState getJobState() {
		return jobState;
	}

	public void setJobState(JobState jobState) {
		this.jobState = jobState;
	}

	public Long getTargetId() {
		return targetId;
	}

	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}

	public Long getAssignedToEntityId() {
		return assignedToEntityId;
	}

	public void setAssignedToEntityId(Long assignedToEntityId) {
		this.assignedToEntityId = assignedToEntityId;
	}

	public float getJobTypeTotalWorkToDo() {
		Float defaultTime = null;
		if (craftingRecipe != null) {
			defaultTime = craftingRecipe.getDefaultTimeToCompleteCrafting();
		} else if (cookingRecipe != null) {
			defaultTime = cookingRecipe.getDefaultTimeToCompleteCooking();
		} else {
			defaultTime = type.getDefaultTimeToCompleteJob();
		}

		if (defaultTime != null) {
			return defaultTime;
		} else {
			return TIME_TO_COMPLETE_JOB_WHEN_UNSPECIFIED;
		}
	}

	public float getWorkDoneSoFar() {
		return workDoneSoFar;
	}

	public void setWorkDoneSoFar(float workDoneSoFar) {
		this.workDoneSoFar = workDoneSoFar;
	}

	public float getTotalWorkToDo() {
		if (overrideWorkDuration != null) {
			return overrideWorkDuration;
		} else {
			return getJobTypeTotalWorkToDo();
		}
	}

	public void setOverrideWorkDuration(Float overrideWorkDuration) {
		this.overrideWorkDuration = overrideWorkDuration;
	}

	public GridPoint2 getJobLocation() {
		return jobLocation;
	}

	public void setJobLocation(GridPoint2 jobLocation) {
		this.jobLocation = jobLocation;
	}

	public Profession getRequiredProfession() {
		return requiredProfession;
	}

	public void setRequiredProfession(Profession requiredProfession) {
		this.requiredProfession = requiredProfession;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Job job = (Job) o;
		return jobId == job.jobId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId);
	}

	public void applyWorkDone(float workDone) {
		this.workDoneSoFar += workDone;
	}

	public JobPriority getJobPriority() {
		return jobPriority;
	}

	public void setJobPriority(JobPriority jobPriority) {
		this.jobPriority = jobPriority;
	}

	public HaulingAllocation getHaulingAllocation() {
		return haulingAllocation;
	}

	public void setHaulingAllocation(HaulingAllocation haulingAllocation) {
		this.haulingAllocation = haulingAllocation;
	}

	public LiquidAllocation getLiquidAllocation() {
		return liquidAllocation;
	}

	public void setLiquidAllocation(LiquidAllocation liquidAllocation) {
		this.liquidAllocation = liquidAllocation;
	}

	public ItemType getRequiredItemType() {
		return requiredItemType;
	}

	public void setRequiredItemType(ItemType requiredItemType) {
		this.requiredItemType = requiredItemType;
	}

	public GridPoint2 getSecondaryLocation() {
		return secondaryLocation;
	}

	public void setSecondaryLocation(GridPoint2 secondaryLocation) {
		this.secondaryLocation = secondaryLocation;
	}

	@Override
	public String toString() {
		return "Job{" +
				"jobId=" + jobId +
				", type=" + type +
				", jobState=" + jobState +
				", priority=" + jobPriority.name() +
				'}';
	}

	public FloorType getReplacementFloorType() {
		return replacementFloorType;
	}

	public void setReplacementFloorType(FloorType replacementFloorType) {
		this.replacementFloorType = replacementFloorType;
	}

	public GameMaterial getReplacementFloorMaterial() {
		return replacementFloorMaterial;
	}

	public void setReplacementFloorMaterial(GameMaterial replacementFloorMaterial) {
		this.replacementFloorMaterial = replacementFloorMaterial;
	}

	public GameMaterial getRequiredItemMaterial() {
		return requiredItemMaterial;
	}

	public void setRequiredItemMaterial(GameMaterial requiredItemMaterial) {
		this.requiredItemMaterial = requiredItemMaterial;
	}

	public CraftingRecipe getCraftingRecipe() {
		return craftingRecipe;
	}

	public void setCraftingRecipe(CraftingRecipe craftingRecipe) {
		this.craftingRecipe = craftingRecipe;
	}

	public CookingRecipe getCookingRecipe() {
		return cookingRecipe;
	}

	public void setCookingRecipe(CookingRecipe cookingRecipe) {
		this.cookingRecipe = cookingRecipe;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.jobs.containsKey(this.jobId)) {
			return;
		}

		JSONObject jobJson = new JSONObject(true);

		jobJson.put("id", jobId);
		jobJson.put("type", type.getName());
		if (!jobState.equals(JobState.POTENTIALLY_ACCESSIBLE)) {
			jobJson.put("state", jobState.name());
		}
		if (!jobPriority.equals(JobPriority.NORMAL)) {
			jobJson.put("priority", jobPriority.name());
		}
		if (targetId != null) {
			jobJson.put("targetId", targetId);
		}
		if (haulingAllocation != null) {
			haulingAllocation.writeTo(savedGameStateHolder);
			jobJson.put("haulingAllocationId", haulingAllocation.getHaulingAllocationId());
		}
		if (liquidAllocation != null) {
			liquidAllocation.writeTo(savedGameStateHolder);
			jobJson.put("liquidAllocation", liquidAllocation.getLiquidAllocationId());
		}

		if (assignedToEntityId != null) {
			jobJson.put("assignedToEntityId", assignedToEntityId);
		}
		jobJson.put("workDone", workDoneSoFar);

		jobJson.put("location", JSONUtils.toJSON(jobLocation));
		if (secondaryLocation != null) {
			jobJson.put("secondaryLocation", JSONUtils.toJSON(secondaryLocation));
		}

		if (requiredProfession != null && !requiredProfession.equals(NULL_PROFESSION)) {
			jobJson.put("profession", requiredProfession.getName());
		}
		if (requiredItemType != null) {
			jobJson.put("itemType", requiredItemType.getItemTypeName());
		}
		if (requiredItemMaterial != null) {
			jobJson.put("material", requiredItemMaterial.getMaterialName());
		}
		if (replacementFloorType != null) {
			jobJson.put("replacementFloorType", replacementFloorType.getFloorTypeName());
		}
		if (replacementFloorMaterial != null) {
			jobJson.put("replacementFloorMaterial", replacementFloorMaterial.getMaterialName());
		}
		if (craftingRecipe != null) {
			jobJson.put("craftingRecipe", craftingRecipe.getRecipeName());
		}
		if (cookingRecipe != null) {
			jobJson.put("cookingRecipe", cookingRecipe.getRecipeName());
		}

		savedGameStateHolder.jobs.put(jobId, this);
		savedGameStateHolder.jobsJson.add(jobJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries dictionaries) throws InvalidSaveException {
		this.type = dictionaries.jobTypeDictionary.getByName(asJson.getString("type"));
		if (this.type == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("type"));
		}

		this.jobId = asJson.getLongValue("id");
		this.jobState = EnumParser.getEnumValue(asJson, "state", JobState.class, JobState.POTENTIALLY_ACCESSIBLE);
		this.jobPriority = EnumParser.getEnumValue(asJson, "priority", JobPriority.class, JobPriority.NORMAL);
		this.targetId = asJson.getLong("targetId");

		Long haulingAllocationId = asJson.getLong("haulingAllocationId");
		if (haulingAllocationId != null) {
			this.haulingAllocation = savedGameStateHolder.haulingAllocations.get(haulingAllocationId);
			if (this.haulingAllocation == null) {
				throw new InvalidSaveException("Could not find hauling allocation with ID " + haulingAllocationId);
			}
		}
		Long liquidAllocationId = asJson.getLong("liquidAllocation");
		if (liquidAllocationId != null) {
			liquidAllocation = savedGameStateHolder.liquidAllocations.get(liquidAllocationId);
			if (liquidAllocation == null) {
				throw new InvalidSaveException("Could not find liquid allocation with ID " + liquidAllocationId);
			}
		}

		this.assignedToEntityId = asJson.getLong("assignedToEntityId");
		this.workDoneSoFar = asJson.getFloatValue("workDone");

		this.jobLocation = JSONUtils.gridPoint2(asJson.getJSONObject("location"));
		this.secondaryLocation = JSONUtils.gridPoint2(asJson.getJSONObject("secondaryLocation"));

		String requiredProfessionName = asJson.getString("profession");
		if (requiredProfessionName != null) {
			this.requiredProfession = dictionaries.professionDictionary.getByName(requiredProfessionName);
			if (this.requiredProfession == null) {
				throw new InvalidSaveException("Could not find profession with name: " + requiredProfessionName);
			}
		} else {
			this.requiredProfession = NULL_PROFESSION;
		}
		String requiredItemTypeName = asJson.getString("itemType");
		if (requiredItemTypeName != null) {
			this.requiredItemType = dictionaries.itemTypeDictionary.getByName(requiredItemTypeName);
			if (this.requiredItemType == null) {
				throw new InvalidSaveException("Could not find item type with name: " + requiredItemTypeName);
			}
		}
		String requiredMaterialName = asJson.getString("material");
		if (requiredMaterialName != null) {
			this.requiredItemMaterial = dictionaries.gameMaterialDictionary.getByName(requiredMaterialName);
			if (this.requiredItemMaterial == null) {
				throw new InvalidSaveException("Could not find material with name: " + requiredMaterialName);
			}
		}
		String replacementFloorTypeName = asJson.getString("replacementFloorType");
		if (replacementFloorTypeName != null) {
			this.replacementFloorType = dictionaries.floorTypeDictionary.getByFloorTypeName(replacementFloorTypeName);
			if (this.replacementFloorType == null) {
				throw new InvalidSaveException("Could not find floor type by name " + replacementFloorTypeName);
			}
		}
		String replacementFloorMaterialName = asJson.getString("replacementFloorMaterial");
		if (replacementFloorMaterialName != null) {
			this.replacementFloorMaterial = dictionaries.gameMaterialDictionary.getByName(replacementFloorMaterialName);
			if (this.replacementFloorMaterial == null) {
				throw new InvalidSaveException("Could not find material with name: " + replacementFloorMaterialName);
			}
		}
		String craftingRecipeName = asJson.getString("craftingRecipe");
		if (craftingRecipeName != null) {
			this.craftingRecipe = dictionaries.craftingRecipeDictionary.getByName(craftingRecipeName);
			if (this.craftingRecipe == null) {
				throw new InvalidSaveException("Could not find crafting recipe with name: " + craftingRecipeName);
			}
		}
		String cookingRecipeName = asJson.getString("cookingRecipe");
		if (cookingRecipeName != null) {
			this.cookingRecipe = dictionaries.cookingRecipeDictionary.getByName(cookingRecipeName);
			if (this.cookingRecipe == null) {
				throw new InvalidSaveException("Could not find cooking recipe with name: " + cookingRecipeName);
			}
		}

		savedGameStateHolder.jobs.put(this.jobId, this);
	}
}
