package technology.rocketjump.undermount.entities.model.physical.item;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.assets.entities.item.model.ItemSize;
import technology.rocketjump.undermount.assets.entities.item.model.ItemStyle;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class ItemEntityAttributes implements EntityAttributes {

	private long seed;
	private EnumMap<GameMaterialType, GameMaterial> materials = new EnumMap<>(GameMaterialType.class);
	private EnumMap<ColoringLayer, Color> otherColors = new EnumMap<>(ColoringLayer.class); // Others such as plant_branches
	private ItemType itemType;
	private ItemSize itemSize = ItemSize.AVERAGE; // Used for displaying larger or smaller than normal item types
	private ItemStyle itemStyle = ItemStyle.DEFAULT;
	private ItemPlacement itemPlacement = ItemPlacement.ON_GROUND;

	private int quantity;

	public ItemEntityAttributes() {

	}

	public ItemEntityAttributes(long seed) {
		this.seed = seed;
	}

	@Override
	public ItemEntityAttributes clone() {
		ItemEntityAttributes cloned = new ItemEntityAttributes(seed);

		for (Map.Entry<GameMaterialType, GameMaterial> entry : this.materials.entrySet()) {
			cloned.materials.put(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<ColoringLayer, Color> entry : this.otherColors.entrySet()) {
			cloned.otherColors.put(entry.getKey(), entry.getValue().cpy());
		}
		cloned.itemType = this.itemType;
		cloned.itemSize = this.itemSize;
		cloned.itemStyle = this.itemStyle;
		cloned.itemPlacement = this.itemPlacement;

		cloned.quantity = this.quantity;

		return cloned;
	}

	@Override
	public Map<GameMaterialType, GameMaterial> getMaterials() {
		return materials;
	}

	@Override
	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		GameMaterialType materialType = coloringLayer.getLinkedMaterialType();
		if (materialType != null) {
			GameMaterial gameMaterial = materials.get(materialType);
			if (gameMaterial != null) {
				return gameMaterial.getColor();
			} else {
				return null;
			}
		} else {
			return otherColors.get(coloringLayer);
		}
	}

	public boolean canMerge(ItemEntityAttributes other) {
		if (this.itemType.equals(other.itemType) &&
				this.itemSize.equals(other.itemSize) &&
				this.itemStyle.equals(other.itemStyle)) {
			GameMaterial primaryMaterial = this.getMaterial(this.itemType.getPrimaryMaterialType());
			GameMaterial otherPrimaryMaterial = other.getMaterial(other.itemType.getPrimaryMaterialType());
			if (primaryMaterial.equals(otherPrimaryMaterial)) {
				return this.quantity + other.quantity <= itemType.getMaxStackSize();
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void setColor(ColoringLayer coloringLayer, Color color) {
		if (color != null) {
			otherColors.put(coloringLayer, color);
		}
	}

	public void setMaterial(GameMaterial material) {
		if (material != null) {
			this.materials.put(material.getMaterialType(), material);
		}
	}

	public GameMaterial getMaterial(GameMaterialType materialType) {
		return materials.get(materialType);
	}

	public void removeMaterial(GameMaterialType gameMaterialType) {
		materials.remove(gameMaterialType);
	}

	public Collection<? extends GameMaterial> getAllMaterials() {
		return materials.values();
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
	}

	public int getQuantity() {
		return quantity;
	}

	// TODO get this to be only called from one place which also manages ItemAllocations
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public ItemSize getItemSize() {
		return itemSize;
	}

	public void setItemSize(ItemSize itemSize) {
		this.itemSize = itemSize;
	}

	public ItemStyle getItemStyle() {
		return itemStyle;
	}

	public void setItemStyle(ItemStyle itemStyle) {
		this.itemStyle = itemStyle;
	}

	public ItemPlacement getItemPlacement() {
		return itemPlacement;
	}

	public void setItemPlacement(ItemPlacement itemPlacement) {
		this.itemPlacement = itemPlacement;
	}

	@Override
	public String toString() {
		return "ItemEntityAttributes{" +
				"itemType=" + itemType +
				", itemSize=" + itemSize +
				", itemStyle=" + itemStyle +
				", itemPlacement=" + itemPlacement +
				", quantity=" + quantity +
				'}';
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		JSONArray materialsJson = new JSONArray();
		for (GameMaterial material : materials.values()) {
			materialsJson.add(material.getMaterialName());
		}
		asJson.put("materials", materialsJson);
		if (!otherColors.isEmpty()) {
			JSONObject otherColorsJson = new JSONObject(true);
			for (Map.Entry<ColoringLayer, Color> entry : otherColors.entrySet()) {
				otherColorsJson.put(entry.getKey().name(), HexColors.toHexString(entry.getValue()));
			}
			asJson.put("otherColors", otherColorsJson);
		}
		asJson.put("type", itemType.getItemTypeName());
		if (!itemSize.equals(ItemSize.AVERAGE)) {
			asJson.put("size", itemSize.name());
		}
		if (!itemStyle.equals(ItemStyle.DEFAULT)) {
			asJson.put("style", itemStyle.name());
		}
		if (!itemPlacement.equals(ItemPlacement.ON_GROUND)) {
			asJson.put("placement", itemPlacement.name());
		}
		if (quantity != 1) {
			asJson.put("quantity", quantity);
		}

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		JSONArray materialsJson = asJson.getJSONArray("materials");
		for (int cursor = 0; cursor < materialsJson.size(); cursor++) {
			GameMaterial material = relatedStores.gameMaterialDictionary.getByName(materialsJson.getString(cursor));
			if (material == null) {
				throw new InvalidSaveException("Could not find material by name " + materialsJson.getString(cursor));
			} else {
				materials.put(material.getMaterialType(), material);
			}
		}
		JSONObject otherColorsJson = asJson.getJSONObject("otherColors");
		if (otherColorsJson != null) {
			for (String coloringLayerName : otherColorsJson.keySet()) {
				ColoringLayer coloringLayer = EnumUtils.getEnum(ColoringLayer.class, coloringLayerName);
				if (coloringLayer == null) {
					throw new InvalidSaveException("Could not find coloring layer by name " + coloringLayerName);
				}
				Color color = HexColors.get(otherColorsJson.getString(coloringLayerName));
				otherColors.put(coloringLayer, color);
			}
		}

		itemType = relatedStores.itemTypeDictionary.getByName(asJson.getString("type"));
		if (itemType == null) {
			throw new InvalidSaveException("Could not find item type by name " + asJson.getString("type"));
		}
		itemSize = EnumParser.getEnumValue(asJson, "size", ItemSize.class, ItemSize.AVERAGE);
		itemStyle = EnumParser.getEnumValue(asJson, "style", ItemStyle.class, ItemStyle.DEFAULT);
		itemPlacement = EnumParser.getEnumValue(asJson, "placement", ItemPlacement.class, ItemPlacement.ON_GROUND);
		Integer quantity = asJson.getInteger("quantity");
		if (quantity == null) {
			this.quantity = 1;
		} else {
			this.quantity = quantity;
		}
	}

	public GameMaterial getPrimaryMaterial() {
		return this.getMaterial(this.getItemType().getPrimaryMaterialType());
	}
}
