package technology.rocketjump.undermount.messaging.types;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.entities.model.Entity;

public class ShedLeavesMessage {
	public final Entity parentEntity;
	public final Color leafColor;

	public ShedLeavesMessage(Entity parentEntity, Color leafColor) {
		this.parentEntity = parentEntity;
		this.leafColor = leafColor;
	}
}
