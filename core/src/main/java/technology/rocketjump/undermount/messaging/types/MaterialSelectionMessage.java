package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

public class MaterialSelectionMessage {

	public final GameMaterialType selectedMaterialType;
	public final GameMaterial selectedMaterial;
	public final ItemType resourceItemType;

	public MaterialSelectionMessage(GameMaterialType selectedMaterialType, GameMaterial selectedMaterial, ItemType resourceItemType) {
		this.selectedMaterialType = selectedMaterialType;
		this.selectedMaterial = selectedMaterial;
		this.resourceItemType = resourceItemType;
	}

}
