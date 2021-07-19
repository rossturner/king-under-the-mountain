package technology.rocketjump.undermount.assets.entities;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.Gdx;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class supplies the information for which layer an entity asset is to be rendered as
 * i.e. it determines what order the sprites are overlaid on each other
 */
@Singleton
public class RenderLayerDictionary {

	private Map<EntityType, Map<EntityAssetOrientation, Map<EntityAssetType, Integer>>> layeringMap = new EnumMap<>(EntityType.class);

	@Inject
	public RenderLayerDictionary(EntityAssetTypeDictionary entityAssetTypeDictionary) throws IOException {
		this(Gdx.files.internal("assets/definitions/entityAssets/renderLayers.json").file(), entityAssetTypeDictionary);
	}

	public RenderLayerDictionary(File renderLayersFile, EntityAssetTypeDictionary entityAssetTypeDictionary) throws IOException {
		JSONObject renderLayersJson = JSON.parseObject(FileUtils.readFileToString(renderLayersFile, "UTF-8"));

		for (String typeString : renderLayersJson.keySet()) {
			if (typeString.startsWith("_")) {
				// This is metadata like _info so ignore it
				continue;
			}
			Map<EntityAssetOrientation, Map<EntityAssetType, Integer>> typeToOrientationMap = new EnumMap<>(EntityAssetOrientation.class);
			JSONObject orientationJson = renderLayersJson.getJSONObject(typeString);

			// Need to ensure non-"sameAs" entries are processed first
			for (String orientationString : orientationJson.keySet()) {
				EntityAssetOrientation orientationValue = EntityAssetOrientation.valueOf(orientationString);
				JSONObject orderingContainer = orientationJson.getJSONObject(orientationString);
				if (orderingContainer.containsKey("ordering")) {
					JSONArray orderingArray = orderingContainer.getJSONArray("ordering");
					int layerCounter = 0;
					Map<EntityAssetType, Integer> layerMap = new LinkedHashMap<>();
					while (layerCounter < orderingArray.size()) {
						layerMap.put(entityAssetTypeDictionary.getByName(orderingArray.getString(layerCounter)), layerCounter);
						layerCounter++;
					}
					typeToOrientationMap.put(orientationValue, layerMap);
				}
			}

			for (String orientationString : orientationJson.keySet()) {
				EntityAssetOrientation orientationValue = EntityAssetOrientation.valueOf(orientationString);
				JSONObject orderingContainer = orientationJson.getJSONObject(orientationString);
				if (orderingContainer.containsKey("sameAs")) {
					EntityAssetOrientation sameAs = EntityAssetOrientation.valueOf(orderingContainer.getString("sameAs"));
					typeToOrientationMap.put(orientationValue, typeToOrientationMap.get(sameAs));
				}
			}

			layeringMap.put(EntityType.valueOf(typeString), typeToOrientationMap);
		}


	}

	public int getRenderingLayer(EntityType entityType, EntityAssetOrientation orientation, EntityAssetType assetType) {
		if (assetType.name.equals(EntityAssetType.UNSPECIFIED)) {
			return 100;
		}
		Map<EntityAssetOrientation, Map<EntityAssetType, Integer>> orientationToAssetTypeMap = layeringMap.get(entityType);
		if (orientationToAssetTypeMap == null) {
			Logger.error("Could not find layering information for entity type: " + entityType.toString() + " in " + this.getClass().getSimpleName());
			return -3;
		}
		Map<EntityAssetType, Integer> assetTypeToLayerMap = orientationToAssetTypeMap.get(orientation);
		if (assetTypeToLayerMap == null) {
			Logger.error("Could not find layering information for orientation " + orientation + " for entity type " + entityType + " in " + this.getClass().getSimpleName());
			return -2;
		}
		Integer layerValue = assetTypeToLayerMap.get(assetType);
		if (layerValue == null) {
			Logger.error("Could not find layer information for asset type " + assetType + " facing " + orientation + " for entity type " + entityType + " in " + this.getClass().getSimpleName());
			return -1;
		}
		return layerValue;
	}

}
