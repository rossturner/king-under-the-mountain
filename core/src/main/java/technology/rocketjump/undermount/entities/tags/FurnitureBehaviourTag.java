package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.behaviour.furniture.*;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.ArrayList;
import java.util.List;

public class FurnitureBehaviourTag extends Tag {

	private static final String ITEM_TYPE_PREFIX = "ItemType_";
	private static final String FURNITURE_TYPE_PREFIX = "FurnitureType_";
	private static final String JOB_TYPE_PREFIX = "JobType_";
	private static final String MATERIAL_PREFIX = "Material_";

	@Override
	public String getTagName() {
		return "FURNITURE_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return EnumUtils.isValidEnum(FurnitureBehaviourName.class, args.get(0));
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getBehaviourComponent() == null) {
			// Don't apply to furniture which already doesn't have a BehaviourComponent e.g. when placing from UI
			return;
		}
		FurnitureBehaviourName behaviourName = FurnitureBehaviourName.valueOf(args.get(0));
		if (!entity.getBehaviourComponent().getClass().equals(behaviourName.behaviourClass)) {
			// Only switch behaviour if already different
			try {
				FurnitureBehaviour furnitureBehaviour = behaviourName.behaviourClass.getDeclaredConstructor().newInstance();
				if (args.size() > 1) {
					List<ItemType> itemTypes = new ArrayList<>();
					List<JobType> jobTypes = new ArrayList<>();
					List<FurnitureType> furnitureTypes = new ArrayList<>();
					List<GameMaterial> materials = new ArrayList<>();

					for (int argsCursor = 1; argsCursor < args.size(); argsCursor++) {
						String arg = args.get(argsCursor);
						if (arg.startsWith(ITEM_TYPE_PREFIX)) {
							arg = arg.substring(ITEM_TYPE_PREFIX.length());
							ItemType itemType = tagProcessingUtils.itemTypeDictionary.getByName(arg);
							if (itemType == null) {
								Logger.error("Could not find item type " + arg + " specified in " + getTagName() + " tag");
							} else {
								itemTypes.add(itemType);
							}
						} else if (arg.startsWith(JOB_TYPE_PREFIX)) {
							arg = arg.substring(JOB_TYPE_PREFIX.length());
							JobType jobType = tagProcessingUtils.jobTypeDictionary.getByName(arg);
							if (jobType == null) {
								Logger.error("Could not find job type with name " + arg + " specified in " + getTagName() + " tag");
							} else {
								jobTypes.add(jobType);
							}
						} else if (arg.startsWith(FURNITURE_TYPE_PREFIX)) {
							arg = arg.substring(FURNITURE_TYPE_PREFIX.length());
							FurnitureType furnitureType = tagProcessingUtils.furnitureTypeDictionary.getByName(arg);
							if (furnitureType == null) {
								Logger.error("Could not find furniture type with name " + arg + " specified in " + getTagName() + " tag");
							} else {
								furnitureTypes.add(furnitureType);
							}
						} else if (arg.startsWith(MATERIAL_PREFIX)) {
							arg = arg.substring(MATERIAL_PREFIX.length());
							GameMaterial material = tagProcessingUtils.materialDictionary.getByName(arg);
							if (material == null) {
								Logger.error("Could not find material with name " + arg + " specified in " + getTagName() + " tag");
							} else {
								materials.add(material);
							}
						} else {
							Logger.error("Unrecognised argument passed to tag " + getTagName() + ": " + arg);
						}
					}
					furnitureBehaviour.setRelatedItemTypes(itemTypes);
					furnitureBehaviour.setRelatedJobTypes(jobTypes);
					furnitureBehaviour.setRelatedFurnitureTypes(furnitureTypes);
					furnitureBehaviour.setRelatedMaterials(materials);
				}
				furnitureBehaviour.init(entity, messageDispatcher, gameContext);
				entity.replaceBehaviourComponent(furnitureBehaviour);
				// As tags are applied during construction, before the ENTITY_CREATED message is triggered which adds them to EntityUpdater
				// Don't need to tell anything that behaviour has changed (in case update per frame or update infrequently is different)
			} catch (ReflectiveOperationException e) {
				Logger.error(e);
			}
		}
	}

	public enum FurnitureBehaviourName {

		// TODO Move these to dictionary in TagProcessingUtils and get by name
		FILL_LIQUID_CONTAINER(FillLiquidContainerBehaviour.class),
		BEER_TAPPER(BeerTapperBehaviour.class),
		WATER_PUMP(WaterPumpBehaviour.class),
		EDIBLE_LIQUID_SOURCE(EdibleLiquidSourceBehaviour.class),
		MUSHROOM_SHOCK_TANK(MushroomShockTankBehaviour.class),
		TRANSFORM_UPON_JOB_COMPLETION(TransformUponJobCompletionFurnitureBehaviour.class),
		TRANSFORM_AFTER_SET_TIME(TransformAfterSetTimeBehaviour.class);

		public final Class<? extends FurnitureBehaviour> behaviourClass;

		FurnitureBehaviourName(Class<? extends FurnitureBehaviour> behaviourClass) {
			this.behaviourClass = behaviourClass;
		}
	}
}
