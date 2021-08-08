package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.OxidisationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;

@Singleton
public class FurnitureEntityFactory {

	private static final float ITEM_RADIUS = 0.4f;
	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;

	@Inject
	public FurnitureEntityFactory(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
	}

	public Entity create(FurnitureEntityAttributes attributes, GridPoint2 tilePosition, BehaviourComponent behaviour, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = createPhysicalComponent(attributes);
		LocationComponent locationComponent = createLocationComponent(tilePosition);

		Entity entity = new Entity(EntityType.FURNITURE, physicalComponent, behaviour, locationComponent, messageDispatcher, gameContext);

		attributes.getMaterials().values().stream().filter(m -> m.getOxidisation() != null)
				.findAny()
				.ifPresent((a) -> {
					OxidisationComponent oxidisationComponent = new OxidisationComponent();
					oxidisationComponent.init(entity, messageDispatcher, gameContext);
					entity.addComponent(oxidisationComponent);
				});


		entityAssetUpdater.updateEntityAssets(entity);
		return entity;
	}

	private LocationComponent createLocationComponent(GridPoint2 tilePosition) {
		LocationComponent locationComponent = new LocationComponent();
		Vector2 worldPosition = new Vector2(tilePosition.x + 0.5f, tilePosition.y + 0.5f);

		locationComponent.setWorldPosition(worldPosition, false);
		locationComponent.setFacing(EntityAssetOrientation.DOWN.toVector2().cpy());
		locationComponent.setRadius(ITEM_RADIUS);
		return locationComponent;
	}

	private PhysicalEntityComponent createPhysicalComponent(EntityAttributes attributes) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);
		return physicalComponent;
	}

}
