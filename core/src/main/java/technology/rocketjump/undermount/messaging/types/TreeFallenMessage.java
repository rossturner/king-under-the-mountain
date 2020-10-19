package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesItem;

import java.util.List;

public class TreeFallenMessage {

	private final Vector2 treeWorldPosition;
	private final Color branchColor;
	private final boolean fallToWest;
	private final List<PlantSpeciesItem> itemsToCreate;

	public TreeFallenMessage(Vector2 treeWorldPosition, Color actualBranchColor, boolean fallToWest, List<PlantSpeciesItem> itemsToCreate) {
		this.treeWorldPosition = treeWorldPosition;
		this.branchColor = actualBranchColor;
		this.fallToWest = fallToWest;
		this.itemsToCreate = itemsToCreate;
	}

	public Vector2 getTreeWorldPosition() {
		return treeWorldPosition;
	}

	public Color getBranchColor() {
		return branchColor;
	}

	public boolean isFallToWest() {
		return fallToWest;
	}

	public List<PlantSpeciesItem> getItemsToCreate() {
		return itemsToCreate;
	}
}
