package technology.rocketjump.undermount.entities.model.physical.furniture;

import technology.rocketjump.undermount.misc.Name;

/**
 * This class represents a higher-level category of furniture than a type e.g. a category is a table or chair
 */
public class FurnitureCategory {

	@Name
	private String name;
	private boolean blocksMovement;
	private boolean impedesMovement;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isBlocksMovement() {
		return blocksMovement;
	}

	public void setBlocksMovement(boolean blocksMovement) {
		this.blocksMovement = blocksMovement;
	}

	public boolean isImpedesMovement() {
		return impedesMovement;
	}

	public void setImpedesMovement(boolean impedesMovement) {
		this.impedesMovement = impedesMovement;
	}

	@Override
	public String toString() {
		return name;
	}

}
