package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.designation.Designation;
import technology.rocketjump.undermount.ui.GameInteractionMode;

public class ApplyDesignationMessage {

	private final MapTile targetTile;
	private final Entity targetEntity;
	private final Designation designationToApply;
	private final GameInteractionMode interactionMode;

	public ApplyDesignationMessage(MapTile targetTile, Designation designationToApply, GameInteractionMode interactionMode) {
		this.targetTile = targetTile;
		this.targetEntity = null;
		this.designationToApply = designationToApply;
		this.interactionMode = interactionMode;
	}

	public ApplyDesignationMessage(Entity targetEntity, Designation designationToApply, GameInteractionMode interactionMode) {
		this.targetTile = null;
		this.targetEntity = targetEntity;
		this.designationToApply = designationToApply;
		this.interactionMode = interactionMode;
	}

	public MapTile getTargetTile() {
		return targetTile;
	}

	public Entity getTargetEntity() {
		return targetEntity;
	}

	public Designation getDesignationToApply() {
		return designationToApply;
	}

	public GameInteractionMode getInteractionMode() {
		return interactionMode;
	}
}
