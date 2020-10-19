package technology.rocketjump.undermount.sprites;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.undermount.jobs.CraftingTypeDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.sprites.model.BridgeType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class BridgeTypeDictionary {

	private final Map<GameMaterialType, BridgeType> byMaterialType = new EnumMap<>(GameMaterialType.class);

	@Inject
	public BridgeTypeDictionary(ItemTypeDictionary itemTypeDictionary, CraftingTypeDictionary craftingTypeDictionary) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		File bridgeTypesJsonFile = new File("assets/definitions/types/bridgeTypes.json");
		List<BridgeType> bridgeTypes = objectMapper.readValue(FileUtils.readFileToString(bridgeTypesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, BridgeType.class));

		for (BridgeType bridgeType : bridgeTypes) {
			QuantifiedItemType requirement = bridgeType.getBuildingRequirement();
			requirement.setItemType(itemTypeDictionary.getByName(requirement.getItemTypeName()));
			if (requirement.getItemType() == null) {
				Logger.error("Could not find item type with name " + requirement.getItemType() + " for bridge of type " + bridgeType.getMaterialType());
				continue;
			}

			bridgeType.setCraftingType(craftingTypeDictionary.getByName(bridgeType.getCraftingTypeName()));
			if (bridgeType.getCraftingType() == null) {
				Logger.error("Could not find crafting type " + bridgeType.getCraftingTypeName() + " for bridge type " + bridgeType.getMaterialType());
				continue;
			}

			byMaterialType.put(bridgeType.getMaterialType(), bridgeType);
		}
	}

	public Set<GameMaterialType> getAllTypes() {
		return byMaterialType.keySet();
	}

	public Collection<BridgeType> getAll() {
		return byMaterialType.values();
	}

	public BridgeType getByMaterialType(GameMaterialType materialType) {
		return byMaterialType.get(materialType);
	}
}
