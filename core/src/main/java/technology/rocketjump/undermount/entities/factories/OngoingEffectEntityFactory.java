package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.behaviour.effects.BaseOngoingEffectBehaviour;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;

import javax.inject.Inject;
import javax.inject.Singleton;

import static technology.rocketjump.undermount.assets.entities.model.NullEntityAsset.NULL_ASSET;
import static technology.rocketjump.undermount.entities.factories.PlantEntityFactory.createPhysicalComponent;

@Singleton
public class OngoingEffectEntityFactory {

	private final MessageDispatcher messageDispatcher;

	@Inject
	public OngoingEffectEntityFactory(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	public Entity create(OngoingEffectAttributes attributes, Vector2 worldPosition, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = createPhysicalComponent(attributes);
		physicalComponent.setBaseAsset(NULL_ASSET);
		BehaviourComponent behaviorComponent = new BaseOngoingEffectBehaviour();
		LocationComponent locationComponent = this.createLocationComponent(worldPosition, attributes);

		Entity entity = new Entity(EntityType.ONGOING_EFFECT, physicalComponent, behaviorComponent, locationComponent, messageDispatcher, gameContext);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity); // to process tags
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
		return entity;
	}

	private LocationComponent createLocationComponent(Vector2 worldPosition, OngoingEffectAttributes attributes) {
		LocationComponent locationComponent = new LocationComponent();
		if (worldPosition != null) {
			locationComponent.setWorldPosition(worldPosition, false);
		}
		locationComponent.setFacing(EntityAssetOrientation.DOWN.toVector2().cpy());
		locationComponent.setRadius(attributes.getEffectRadius());
		return locationComponent;
	}
}
