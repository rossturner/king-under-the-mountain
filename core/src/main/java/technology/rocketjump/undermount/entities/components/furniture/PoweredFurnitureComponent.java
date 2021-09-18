package technology.rocketjump.undermount.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class PoweredFurnitureComponent implements ParentDependentEntityComponent, Destructible {

	private Entity parentEntity;
	private GameContext gameContext;
	private int powerAmount;
	private float animationSpeed;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.gameContext = gameContext;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		PoweredFurnitureComponent clone = new PoweredFurnitureComponent();
		clone.powerAmount = this.powerAmount;
		clone.animationSpeed = this.animationSpeed;
		return clone;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.powerAmount = 0;
		updatePowerGridAtParentLocation();
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		parentTile.getOrCreateUnderTile().setPowerConsumer(false);
	}

	public void update(float deltaTime, GameContext gameContext) {
		// TODO actually apply power usage

		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		parentTile.getOrCreateUnderTile().setPowerConsumer(true);

		float animationProgress = parentEntity.getPhysicalEntityComponent().getAnimationProgress();
		animationProgress += deltaTime * animationSpeed;
		while (animationProgress > 1f) {
			animationProgress -= 1f;
		}
		parentEntity.getPhysicalEntityComponent().setAnimationProgress(animationProgress);
	}

	public void updatePowerGridAtParentLocation() {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null && parentTile.getUnderTile() != null && parentTile.getUnderTile().getPowerGrid() != null) {
			parentTile.getUnderTile().getPowerGrid().update();
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("powerAmount", powerAmount);
		asJson.put("animationSpeed", animationSpeed);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.powerAmount = asJson.getIntValue("powerAmount");
		this.animationSpeed = asJson.getFloatValue("animationSpeed");
	}

	public void setPowerAmount(int powerAmount) {
		this.powerAmount = powerAmount;
	}

	public int getPowerAmount() {
		return powerAmount;
	}

	public void setAnimationSpeed(float animationSpeed) {
		this.animationSpeed = animationSpeed;
	}

	public float getAnimationSpeed() {
		return animationSpeed;
	}
}
