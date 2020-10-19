package technology.rocketjump.undermount.entities.components.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesItem;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HarvestableEntityComponent implements EntityComponent {

	List<PlantSpeciesItem> harvestableItems = new ArrayList<>();

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		HarvestableEntityComponent cloned = new HarvestableEntityComponent();
		cloned.harvestableItems.addAll(this.harvestableItems);
		return cloned;
	}


	public void add(PlantSpeciesItem plantSpeciesItem) {
		harvestableItems.add(plantSpeciesItem);
	}

	public void clear() {
		harvestableItems.clear();
	}

	public boolean isEmpty() {
		return harvestableItems.isEmpty();
	}

	public Collection<PlantSpeciesItem> getAll() {
		return harvestableItems;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!harvestableItems.isEmpty()) {
			JSONArray itemsJson = new JSONArray();
			for (PlantSpeciesItem harvestableItem : harvestableItems) {
				JSONObject harvestableItemJson = new JSONObject(true);
				harvestableItem.writeTo(harvestableItemJson, savedGameStateHolder);
				itemsJson.add(harvestableItemJson);
			}
			asJson.put("items", itemsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray itemsJson = asJson.getJSONArray("items");
		if (itemsJson != null) {
			for (int cursor = 0; cursor < itemsJson.size(); cursor++) {
				JSONObject itemJson = itemsJson.getJSONObject(cursor);
				PlantSpeciesItem harvestItem = new PlantSpeciesItem();
				harvestItem.readFrom(itemJson, savedGameStateHolder, relatedStores);
				harvestableItems.add(harvestItem);
			}
		}
	}
}
