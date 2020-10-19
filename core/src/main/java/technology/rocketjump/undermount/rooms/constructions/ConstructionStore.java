package technology.rocketjump.undermount.rooms.constructions;

import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.MapTile;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ConstructionStore implements GameContextAware {

	private static final List<Construction> emptyArray = new ArrayList<>();

	private List<Construction> iterableArray = new ArrayList<>();
	private GameContext gameContext;
	private int iterationCursor = 0;

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

	public Construction next() {
		if (iterationCursor >= iterableArray.size()) {
			if (gameContext.getConstructions().size() > 0) {
				iterableArray = new ArrayList<>(gameContext.getConstructions().values());
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
