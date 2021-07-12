package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class FloorConstructionMessage {
	public final GridPoint2 location;
	public final ItemType constructionItem;
	public final GameMaterial constructionMaterial;

	public FloorConstructionMessage(GridPoint2 location, ItemType constructionItem, GameMaterial constructionMaterial) {
		this.location = location;
		this.constructionItem = constructionItem;
		this.constructionMaterial = constructionMaterial;
	}
}
