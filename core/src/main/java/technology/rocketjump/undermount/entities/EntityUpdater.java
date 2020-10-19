package technology.rocketjump.undermount.entities;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;

import java.util.List;

@Singleton
public class EntityUpdater implements Updatable {

	private int infrequentUpdateCursor = 0;

	public static final float TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS = 3f;//1f / 30f;
	private final EntityStore entityStore;
	private GameContext gameContext;

	@Inject
	public EntityUpdater(EntityStore entityStore) {
		this.entityStore = entityStore;
	}

	@Override
	public void update(float deltaTime) {
		for (Entity updatableEntity : entityStore.getUpdateEveryFrameEntities()) {
			if (updatableEntity != null) { // FIXME No idea how this is sometimes being null
				updatableEntity.update(deltaTime, gameContext);
			}
		}

		List<Entity> infrequentUpdateEntities = entityStore.getUpdateInfrequentlyEntities();
		float numInfrequentEntitiesToUpdatePerSecond = infrequentUpdateEntities.size() / TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS;
		int numEntitiesToUpdateThisFrame = Math.round(deltaTime * numInfrequentEntitiesToUpdatePerSecond);
		numEntitiesToUpdateThisFrame = Math.max(1, numEntitiesToUpdateThisFrame);

		if (!infrequentUpdateEntities.isEmpty()) {
			while (numEntitiesToUpdateThisFrame > 0) {
				if (infrequentUpdateCursor >= infrequentUpdateEntities.size()) {
					infrequentUpdateCursor = 0;
				}

				Entity entity = infrequentUpdateEntities.get(infrequentUpdateCursor);
				// Note that this can end up being called after an entity has been removed from infrequentUpdateEntities
				entity.infrequentUpdate(gameContext);
				infrequentUpdateCursor++;
				numEntitiesToUpdateThisFrame--;
			}
		}
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.infrequentUpdateCursor = 0;
	}
}
