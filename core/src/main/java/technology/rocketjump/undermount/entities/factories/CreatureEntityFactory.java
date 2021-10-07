package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.ai.goap.GoalDictionary;
import technology.rocketjump.undermount.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.undermount.entities.behaviour.DoNothingBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rooms.RoomStore;

@Singleton
public class CreatureEntityFactory  {

	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final GoalDictionary goalDictionary;
	private final ScheduleDictionary scheduleDictionary;
	private final RoomStore roomStore;

	@Inject
	public CreatureEntityFactory(CreatureEntityAttributesFactory creatureEntityAttributesFactory,
								 MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater, GoalDictionary goalDictionary,
								 ScheduleDictionary scheduleDictionary, RoomStore roomStore) {
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
		this.goalDictionary = goalDictionary;
		this.scheduleDictionary = scheduleDictionary;
		this.roomStore = roomStore;
	}

	public Entity create(CreatureEntityAttributes attributes, Vector2 worldPosition, Vector2 facing, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);

		BehaviourComponent behaviourComponent = new DoNothingBehaviour();
		if (attributes.getRace().getBehaviour().getBehaviourClass() != null) {
			try {
				behaviourComponent = attributes.getRace().getBehaviour().getBehaviourClass().getConstructor().newInstance();

				if (behaviourComponent instanceof SettlerBehaviour) {
					SettlerBehaviour settlerBehaviour = (SettlerBehaviour) behaviourComponent;
					settlerBehaviour.constructWith(goalDictionary, scheduleDictionary, roomStore);
				}

			} catch (ReflectiveOperationException e) {
				Logger.error("Could not initialise behaviour class " + attributes.getRace().getBehaviour().getBehaviourClass().getSimpleName());
			}
		}

		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setWorldPosition(worldPosition, true);
		locationComponent.setFacing(facing);

		Entity entity = new Entity(EntityType.CREATURE, physicalComponent, behaviourComponent, locationComponent,
				messageDispatcher, gameContext);

		entityAssetUpdater.updateEntityAssets(entity);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
		return entity;
	}

}
