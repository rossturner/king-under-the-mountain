package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.furniture.PoweredFurnitureComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class PowerSourceBehaviour extends FurnitureBehaviour implements Destructible {

	private boolean powered = true; // TODO introduce power

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		PoweredFurnitureComponent poweredFurnitureComponent = parentEntity.getComponent(PoweredFurnitureComponent.class);
		if (poweredFurnitureComponent != null) {
			poweredFurnitureComponent.update(deltaTime, gameContext);
		}


		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null) {
			UnderTile underTile = parentTile.getOrCreateUnderTile();
			underTile.setPowerSource(true);

			// TODO add gear to undertile
		}
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null) {
			UnderTile underTile = parentTile.getUnderTile();
			if (underTile != null) {
				underTile.setPowerSource(false);

				// TODO remove gear from undertile
			}
		}
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (powered) {
			asJson.put("powered", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.powered = asJson.getBooleanValue("powered");
	}

}
