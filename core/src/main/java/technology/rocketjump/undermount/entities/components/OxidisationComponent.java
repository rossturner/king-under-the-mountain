package technology.rocketjump.undermount.entities.components;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.OxidisationMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OxidisationComponent implements InfrequentlyUpdatableComponent {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	private Map<GameMaterial, Double> timeSpentOxidisingByMaterial = new HashMap<>();

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		OxidisationComponent clone = new OxidisationComponent();
		clone.messageDispatcher = messageDispatcher;
		clone.gameContext = gameContext;

		clone.timeSpentOxidisingByMaterial.putAll(this.timeSpentOxidisingByMaterial);

		return clone;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
	}

	@Override
	public void infrequentUpdate(double elapsedTime) {
		if (gameContext.getMapEnvironment().getCurrentWeather().isOxidises()) {
			MapTile currentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition());
			if (currentTile == null ||  !currentTile.getRoof().getState().equals(TileRoofState.OPEN)) {
				// Don't oxidise unless outside
				return;
			}

			EntityAttributes attributes = parentEntity.getPhysicalEntityComponent().getAttributes();
			Collection<GameMaterial> parentMaterials = List.of();
			if (attributes instanceof ItemEntityAttributes) {
				parentMaterials = ((ItemEntityAttributes)attributes).getAllMaterials();

				if (parentEntity.getLocationComponent().getContainerEntity() != null) {
					InventoryComponent inventoryComponent = parentEntity.getLocationComponent().getContainerEntity().getComponent(InventoryComponent.class);
					if (inventoryComponent != null &&inventoryComponent.getById(parentEntity.getId()) != null) {
						// This item is within a parent's InventoryComponent, so for now will not oxidise
						// Note that this doesn't apply to HaulingComponent, EquippedItemComponent or DecorationInventoryComponent
						return;
					}
				}
				if (((ItemEntityAttributes)attributes).isDestroyed()) {
					return;
				}

			} else if (attributes instanceof FurnitureEntityAttributes) {
				if (((FurnitureEntityAttributes)attributes).isDestroyed()) {
					return;
				}
				parentMaterials = ((FurnitureEntityAttributes)attributes).getMaterials().values();
			} else {
				Logger.warn("Not yet implemented: OxidisationComponent update for " + attributes.getClass().getSimpleName());
			}

			parentMaterials.stream()
					.filter(m -> m.getOxidisation() != null)
					.forEach(material -> {
						double timeSpentOxidising = timeSpentOxidisingByMaterial.getOrDefault(material, 0.0);
						timeSpentOxidising += elapsedTime;
						timeSpentOxidisingByMaterial.put(material, timeSpentOxidising);

						if (timeSpentOxidising > material.getOxidisation().getHoursToConvert()) {
							messageDispatcher.dispatchMessage(MessageType.MATERIAL_OXIDISED, new OxidisationMessage(
									parentEntity, material
							));
							timeSpentOxidisingByMaterial.remove(material);
						}
					});


		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONObject timeSpentJson = new JSONObject(true);
		for (Map.Entry<GameMaterial, Double> entry : timeSpentOxidisingByMaterial.entrySet()) {
			timeSpentJson.put(entry.getKey().getMaterialName(), entry.getValue());
		}
		asJson.put("timeSpent", timeSpentJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject timeSpentJson = asJson.getJSONObject("timeSpent");
		if (timeSpentJson == null) {
			throw new InvalidSaveException("No timeSpent json object for " + getClass().getSimpleName());
		}

		for (String materialName : timeSpentJson.keySet()) {
			double time = timeSpentJson.getDoubleValue(materialName);
			GameMaterial material  = relatedStores.gameMaterialDictionary.getByName(materialName);
			if (materialName == null) {
				throw new InvalidSaveException("Could not find material with name " + materialName + " for " + getClass().getSimpleName());
			}
			this.timeSpentOxidisingByMaterial.put(material, time);
		}

	}
}
