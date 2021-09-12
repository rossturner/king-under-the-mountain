package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.TileLiquidFlow;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class WellBehaviour extends FurnitureBehaviour implements Destructible {

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent == null) {
			Logger.error(this.getClass().getSimpleName() + " does not have a " + LiquidContainerComponent.class.getSimpleName());
		} else if (liquidContainerComponent.getTargetLiquidMaterial() == null) {
			Logger.error(this.getClass().getSimpleName() + " does not have a specified target liquid material");
		}
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null) {
			UnderTile underTile = parentTile.getOrCreateUnderTile();
			underTile.setLiquidOutput(false);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent == null) {
			Logger.error("No " + LiquidContainerComponent.class.getSimpleName() + " for " + this.getClass().getSimpleName());
			return;
		}

		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null) {
			UnderTile underTile = parentTile.getOrCreateUnderTile();
			underTile.setLiquidOutput(true);

			float availableLiquidCapacity = ((float)liquidContainerComponent.getMaxLiquidCapacity()) - liquidContainerComponent.getLiquidQuantity();
			if (availableLiquidCapacity >= 1f) {
				TileLiquidFlow liquidFlow = underTile.getLiquidFlow();
				if (liquidFlow != null && liquidFlow.getLiquidAmount() > 0) {
					consumeLiquid(parentTile, liquidFlow, liquidContainerComponent);
				}
			}
		}
	}

	private void consumeLiquid(MapTile parentTile, TileLiquidFlow liquidFlow, LiquidContainerComponent liquidContainerComponent) {
		int amountInTile = liquidFlow.getLiquidAmount();
		liquidContainerComponent.setTargetLiquidMaterial(liquidFlow.getLiquidMaterial());
		liquidContainerComponent.setLiquidQuantity(liquidContainerComponent.getLiquidQuantity() + amountInTile);
		if (liquidContainerComponent.getLiquidQuantity() > liquidContainerComponent.getMaxLiquidCapacity()) {
			liquidContainerComponent.setLiquidQuantity(liquidContainerComponent.getMaxLiquidCapacity());
		}

		messageDispatcher.dispatchMessage(MessageType.LIQUID_REMOVED_FROM_FLOW, parentTile.getTilePosition());
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
