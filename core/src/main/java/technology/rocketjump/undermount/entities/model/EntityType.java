package technology.rocketjump.undermount.entities.model;

import java.util.List;

public enum EntityType {


	CREATURE,
	PLANT, ITEM, FURNITURE,
	ONGOING_EFFECT, MECHANISM;

	public static final List<EntityType> STATIC_ENTITY_TYPES = List.of(PLANT, ITEM, FURNITURE);
}
