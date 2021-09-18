package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.furniture.PoweredFurnitureComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.undermount.mapping.tile.underground.TileLiquidFlow.MAX_LIQUID_FLOW_PER_TILE;

public class WaterPumpBehaviour extends FurnitureBehaviour implements Destructible {

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		PoweredFurnitureComponent poweredFurnitureComponent = parentEntity.getComponent(PoweredFurnitureComponent.class);
		if (poweredFurnitureComponent != null) {
			poweredFurnitureComponent.update(deltaTime, gameContext);

			MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
			if (parentTile != null) {
				UnderTile underTile = parentTile.getOrCreateUnderTile();
				underTile.setLiquidSource(true);

				if (poweredFurnitureComponent.isPowered(underTile)) {
					if (underTile.liquidCanFlow()) {
						if (underTile.getOrCreateLiquidFlow().getLiquidAmount() < MAX_LIQUID_FLOW_PER_TILE) {
							while (underTile.getOrCreateLiquidFlow().getLiquidAmount() < MAX_LIQUID_FLOW_PER_TILE) {
								messageDispatcher.dispatchMessage(MessageType.ADD_LIQUID_TO_FLOW, parentTile);
							}
						} else {
							// trigger once to force activation, hopefully remove this when active liquid tiles are part of game state
							messageDispatcher.dispatchMessage(MessageType.ADD_LIQUID_TO_FLOW, parentTile);
						}
					}
				}
			}
		}


	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null) {
			UnderTile underTile = parentTile.getUnderTile();
			if (underTile != null) {
				underTile.setLiquidSource(false);
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
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
	}

}
