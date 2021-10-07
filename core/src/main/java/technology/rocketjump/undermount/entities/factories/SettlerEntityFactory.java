package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.EntityAssetUpdater;
import technology.rocketjump.undermount.entities.ai.goap.GoalDictionary;
import technology.rocketjump.undermount.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.undermount.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.Profession;
import technology.rocketjump.undermount.rooms.RoomStore;

@Singleton
public class SettlerEntityFactory {

	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final ProfessionDictionary professionDictionary;
	private final GoalDictionary goalDictionary;
	private final ScheduleDictionary scheduleDictionary;
	private final RoomStore roomStore;

	@Inject
	public SettlerEntityFactory(MessageDispatcher messageDispatcher, ProfessionDictionary professionDictionary,
								EntityAssetUpdater entityAssetUpdater, GoalDictionary goalDictionary, ScheduleDictionary scheduleDictionary, RoomStore roomStore) {
		this.messageDispatcher = messageDispatcher;
		this.professionDictionary = professionDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
		this.goalDictionary = goalDictionary;
		this.scheduleDictionary = scheduleDictionary;
		this.roomStore = roomStore;
	}

	public Entity create(CreatureEntityAttributes attributes, Vector2 worldPosition, Vector2 facing, Profession primaryProfession,
						 Profession secondaryProfession, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);

		SettlerBehaviour behaviourComponent = new SettlerBehaviour();
		behaviourComponent.constructWith(goalDictionary, scheduleDictionary, roomStore);

		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setWorldPosition(worldPosition, true);

		Entity entity = new Entity(EntityType.CREATURE, physicalComponent, behaviourComponent, locationComponent,
				messageDispatcher, gameContext);
		entity.addComponent(new HaulingComponent());
		locationComponent.setFacing(facing);

		ProfessionsComponent professionsComponent = new ProfessionsComponent();
		if (primaryProfession == null) {
			primaryProfession = professionDictionary.getDefault();
		}
		professionsComponent.add(primaryProfession, 0.5f);
		if (secondaryProfession != null && !secondaryProfession.equals(primaryProfession)) {
			professionsComponent.add(secondaryProfession, 0.25f);
		}
		entity.addComponent(professionsComponent);

		NeedsComponent needsComponent = new NeedsComponent(gameContext.getRandom());
		entity.addComponent(needsComponent);
		entity.addComponent(new MemoryComponent());

		entityAssetUpdater.updateEntityAssets(entity);

		return entity;
	}

}
