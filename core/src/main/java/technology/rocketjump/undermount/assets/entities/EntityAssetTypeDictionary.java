package technology.rocketjump.undermount.assets.entities;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class supplies the information for which layer an entity asset is to be rendered as
 * i.e. it determines what order the sprites are overlaid on each other
 */
@Singleton
public class EntityAssetTypeDictionary {

	private final Map<EntityType, Map<String, EntityAssetType>> byEntityType = new HashMap<>();

	private final List<EntityAssetType> all = new ArrayList<>();
	private final Map<String, EntityAssetType> allByName = new HashMap<>();

	@Inject
	public EntityAssetTypeDictionary() throws IOException {
		this(new File("assets/definitions/entityAssets/entityAssetTypes.json"));
	}

	public EntityAssetTypeDictionary(File assetTypesJsonFile) throws IOException {
		JSONObject assetTypesJson = JSON.parseObject(FileUtils.readFileToString(assetTypesJsonFile, "UTF-8"));

		for (String typeString : assetTypesJson.keySet()) {
			if (typeString.startsWith("_")) {
				// This is metadata like _info so ignore it
				continue;
			}

			Map<String, EntityAssetType> byName = new HashMap<>();

			JSONArray names = assetTypesJson.getJSONArray(typeString);
			for (int cursor = 0; cursor < names.size(); cursor++) {
				EntityAssetType entityAssetType = new EntityAssetType(names.getString(cursor));
				byName.put(entityAssetType.name, entityAssetType);
				all.add(entityAssetType);
				if (allByName.containsKey(entityAssetType.name)) {
					throw new RuntimeException("Multiple entity asset types with same name: " + entityAssetType.name);
				}
				allByName.put(entityAssetType.name, entityAssetType);
			}

			EntityType entityType = EntityType.valueOf(typeString);
			byEntityType.put(entityType, byName);
		}
	}

	public List<EntityAssetType> getAll() {
		return all;
	}

	public EntityAssetType getByName(String name) {
		return allByName.get(name);
	}

	public Collection<EntityAssetType> getByEntityType(EntityType entityType) {
		if (!byEntityType.containsKey(entityType)) {
			return Collections.emptyList();
		}
		return byEntityType.get(entityType).values();
	}

}
