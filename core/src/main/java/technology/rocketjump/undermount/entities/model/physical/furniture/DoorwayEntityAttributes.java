package technology.rocketjump.undermount.entities.model.physical.furniture;

import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class DoorwayEntityAttributes extends FurnitureEntityAttributes {

	private WallType attachedWallType; // FIXME #75 something to say what kind/material of wall it is attached to
	private EntityAssetOrientation orientation; // Marks out which edge the wall cap / this entity is against

	public DoorwayEntityAttributes(long seed) {
		super(seed);
	}

	public WallType getAttachedWallType() {
		return attachedWallType;
	}

	public void setAttachedWallType(WallType attachedWallType) {
		this.attachedWallType = attachedWallType;
	}

	public GameMaterial getAttachedWallMaterial() {
		return materials.get(primaryMaterialType);
	}

	public void setAttachedWallMaterial(GameMaterial attachedWallMaterial) {
		this.materials.put(attachedWallMaterial.getMaterialType(), attachedWallMaterial);
		this.primaryMaterialType = attachedWallMaterial.getMaterialType();
	}

	public EntityAssetOrientation getOrientation() {
		return orientation;
	}

	public void setOrientation(EntityAssetOrientation orientation) {
		this.orientation = orientation;
	}

	@Override
	public EntityAttributes clone() {
		throw new NotImplementedException("clone() not yet implemented for " + this.getClass().getSimpleName());
	}

	@Override
	public String toString() {
		return "DoorwayEntityAttributes{" +
				"attachedWallType=" + attachedWallType +
				", orientation=" + orientation +
				", materials=" + materials +
				", primaryMaterialType=" + primaryMaterialType +
				'}';
	}
}
