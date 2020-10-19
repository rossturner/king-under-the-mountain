package technology.rocketjump.undermount.entities.model.physical.item;

public enum ItemGroup {

	RESOURCE, TOOL, PRODUCT, FUEL,
	INGREDIENT_WITH_ITEM_NAME,
	INGREDIENT; // Special case where item type not described to player, just material, e.g. 10 tomatoes rather than 10 tomato sacks

	public String getI18nKey() {
		return "ITEM_GROUP." + name();
	}

}
