package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.behaviour.DoNothingBehaviour;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;

@Singleton
public class CreatureEntityFactory  {

	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;

	@Inject
	public CreatureEntityFactory(CreatureEntityAttributesFactory creatureEntityAttributesFactory, MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater) {
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
	}

	public Entity create(CreatureEntityAttributes attributes, Vector2 worldPosition, Vector2 facing, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);

//		BehaviourComponent behaviourComponent = new SettlerBehaviour(goalDictionary, scheduleDictionary, roomStore);
		// TODO select behaviour

		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setWorldPosition(worldPosition, true);
		locationComponent.setFacing(facing);

		Entity entity = new Entity(EntityType.CREATURE, physicalComponent, new DoNothingBehaviour(), locationComponent,
				messageDispatcher, gameContext);

		entityAssetUpdater.updateEntityAssets(entity);

		return entity;
	}

}
