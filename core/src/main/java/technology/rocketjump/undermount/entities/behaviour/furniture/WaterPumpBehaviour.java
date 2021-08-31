package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Lists;
import technology.rocketjump.undermount.entities.components.furniture.PoweredFurnitureComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Gender;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWord;

import java.util.List;
import java.util.Map;

import static technology.rocketjump.undermount.mapping.tile.underground.TileLiquidFlow.MAX_LIQUID_FLOW_PER_TILE;

public class WaterPumpBehaviour extends FurnitureBehaviour implements Destructible, SelectableDescription {

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
			underTile.setLiquidInput(true);

			if (powered) {
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

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null) {
			UnderTile underTile = parentTile.getUnderTile();
			if (underTile != null) {
				underTile.setLiquidInput(false);
			}
		}
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		I18nWord descriptionWord = i18nTranslator.getDictionary().getWord("TODO");
		return Lists.newArrayList(i18nTranslator.applyReplacements(descriptionWord, Map.of(), Gender.ANY));
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
