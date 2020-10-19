package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class ItemPrimaryMaterialChangedMessage {

    public final Entity item;
    public final GameMaterial oldPrimaryMaterial;

    public ItemPrimaryMaterialChangedMessage(Entity item, GameMaterial oldPrimaryMaterial) {
        this.item = item;
        this.oldPrimaryMaterial = oldPrimaryMaterial;
    }
}
