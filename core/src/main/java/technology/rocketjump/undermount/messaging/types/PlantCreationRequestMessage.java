package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.ai.goap.actions.EntityCreatedCallback;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class PlantCreationRequestMessage {

	private final GameMaterial seedMaterial;
	private final EntityCreatedCallback callback;

	public PlantCreationRequestMessage(GameMaterial seedMaterial, EntityCreatedCallback callback) {
		this.seedMaterial = seedMaterial;
		this.callback = callback;
	}

	public GameMaterial getSeedMaterial() {
		return seedMaterial;
	}

	public EntityCreatedCallback getCallback() {
		return callback;
	}
}
