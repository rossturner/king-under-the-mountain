package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.jobs.model.Profession;

public class RequestHaulingMessage {

	public final Entity itemToBeMoved;
	public final Entity requestingEntity;
	public final boolean forceHaulingEvenWithoutStockpile;
	public final JobCreatedCallback callback;

	private Profession specificProfessionRequired;

	public RequestHaulingMessage(Entity itemToBeMoved, Entity requestingEntity, boolean forceHaulingEvenWithoutStockpile, JobCreatedCallback callback) {
		this.itemToBeMoved = itemToBeMoved;
		this.requestingEntity = requestingEntity;
		this.forceHaulingEvenWithoutStockpile = forceHaulingEvenWithoutStockpile;
		this.callback = callback;
	}

	public Entity getItemToBeMoved() {
		return itemToBeMoved;
	}

	public boolean forceHaulingEvenWithoutStockpile() {
		return forceHaulingEvenWithoutStockpile;
	}

	public Profession getSpecificProfessionRequired() {
		return specificProfessionRequired;
	}

	public void setSpecificProfessionRequired(Profession specificProfessionRequired) {
		this.specificProfessionRequired = specificProfessionRequired;
	}

}
