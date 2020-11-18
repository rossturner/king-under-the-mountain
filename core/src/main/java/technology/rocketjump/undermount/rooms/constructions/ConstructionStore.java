package technology.rocketjump.undermount.rooms.constructions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.mapping.tile.MapTile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ConstructionStore implements GameContextAware {

	private static final List<Construction> emptyArray = new ArrayList<>();
	private final MessageDispatcher messageDispatcher;

	private List<Construction> iterableArray = new ArrayList<>();
	private GameContext gameContext;
	private int iterationCursor = 0;

	@Inject
	public ConstructionStore(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	public void create(Construction construction) {

		for (GridPoint2 tileLocation : construction.getTileLocations()) {
			MapTile tile = gameContext.getAreaMap().getTile(tileLocation);
			if (tile == null) {
				Logger.error("Placing construction to null tile");
				return;
			} else if (tile.hasConstruction()) {
				Logger.error("ERROR: Construction already exists in target tile");
				return;
			} else {
				tile.setConstruction(construction);
			}

			if (tile.hasRoom()) {
				if (tile.getRoomTile().getRoom().getBehaviourComponent() instanceof Prioritisable) {
					Prioritisable prioritisable = (Prioritisable) tile.getRoomTile().getRoom().getBehaviourComponent();
					if (!prioritisable.getPriority().equals(JobPriority.NORMAL)) {
						construction.setPriority(prioritisable.getPriority(), messageDispatcher);
					}
				}
			}
		}

		gameContext.getConstructions().put(construction.getId(), construction);
	}

	/**
	 * This is only updated by something (ConstructionManager) calling next()
	 */
	public List<Construction> getAll() {
		return iterableArray;
	}

	public void addExisting(Construction construction) {
		gameContext.getConstructions().put(construction.getId(), construction);
	}

	public void remove(Construction construction) {
		gameContext.getConstructions().remove(construction.getId());
		for (GridPoint2 tileLocation : construction.getTileLocations()) {
			MapTile tile = gameContext.getAreaMap().getTile(tileLocation);
			if (tile != null) {
				tile.setConstruction(null);
			}
		}
		construction.setState(ConstructionState.REMOVED);
	}

	public void priorityChanged() {
		// Set to end of list to force refresh next update
		iterationCursor = iterableArray.size();
	}

	public Construction next() {
		if (iterationCursor >= iterableArray.size()) {
			if (gameContext.getConstructions().size() > 0) {
				iterableArray = constructionsInPriorityOrder();
			} else {
				iterableArray = emptyArray;
			}
			iterationCursor = 0;
		}
		if (iterableArray.size() == 0) {
			return null;
		} else {
			Construction construction = iterableArray.get(iterationCursor);
			iterationCursor++;
			return construction;
		}
	}

	private List<Construction> constructionsInPriorityOrder() {
		List<Construction> ordered = new ArrayList<>(gameContext.getConstructions().values().size());

		Map<JobPriority, List<Construction>> byPriority = new HashMap<>();
		for (Construction construction : gameContext.getConstructions().values()) {
			byPriority.computeIfAbsent(construction.priority, a -> new ArrayList<>()).add(construction);
		}

		for (JobPriority priority : JobPriority.values()) {
			if (priority.equals(JobPriority.DISABLED)) {
				continue;
			}
			ordered.addAll(byPriority.getOrDefault(priority, emptyArray));
		}

		return ordered;
	}

	public Construction getById(Long targetConstructionId) {
		return gameContext.getConstructions().get(targetConstructionId);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		iterationCursor = 0;
		iterableArray.clear();
	}
}
