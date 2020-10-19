package technology.rocketjump.undermount.entities.behaviour.plants;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.EntityMessage;
import technology.rocketjump.undermount.messaging.types.TreeFallenMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class FallingTreeBehaviour implements BehaviourComponent {

	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;

	private boolean fallToWest;
	private float absoluteRotationAmount = 0f;

	public FallingTreeBehaviour() {

	}

	public FallingTreeBehaviour(boolean fallToWest) {
		this.fallToWest = fallToWest;
	}

	@Override
	public FallingTreeBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		FallingTreeBehaviour cloned = new FallingTreeBehaviour(fallToWest);
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
	}

	private static final float ROTATION_DEGREES_PER_SECOND = 120f;

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		PlantEntityAttributes attributes = (PlantEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();

		float rotationChange = 0.05f + (0.9f * (absoluteRotationAmount / 90));
		float extraRotation = deltaTime * rotationChange * ROTATION_DEGREES_PER_SECOND;

		absoluteRotationAmount += extraRotation;

		if (fallToWest) {
			parentEntity.getLocationComponent().setRotation(absoluteRotationAmount);
		} else {
			parentEntity.getLocationComponent().setRotation(-absoluteRotationAmount);
		}

		if (absoluteRotationAmount > 85f) {
			// Tree has collapsed
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, new EntityMessage(parentEntity.getId()));

			PlantSpeciesGrowthStage currentGrowthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());
			messageDispatcher.dispatchMessage(MessageType.TREE_FELLED, new TreeFallenMessage(
					parentEntity.getLocationComponent().getWorldPosition(), attributes.getColor(ColoringLayer.BRANCHES_COLOR),
					fallToWest, currentGrowthStage.getHarvestedItems()));
		}

	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		// Do nothing, is not infrequent updater
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return false;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (fallToWest) {
			asJson.put("fallToWest", true);
		}
		asJson.put("rotation", absoluteRotationAmount);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.fallToWest = asJson.getBooleanValue("fallToWest");
		this.absoluteRotationAmount = asJson.getFloatValue("rotation");
	}
}
