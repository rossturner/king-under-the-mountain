package technology.rocketjump.undermount.mapping;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.tags.SupportsRoofTag;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.RoofConstructionState;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;

import javax.inject.Inject;
import javax.inject.Singleton;

import static technology.rocketjump.undermount.mapping.tile.roof.RoofConstructionState.*;

@Singleton
public class RoofConstructionManager implements GameContextAware {

	private GameContext gameContext;
	private final int ROOF_SUPPORT_MAX_DISTANCE;

	@Inject
	public RoofConstructionManager(ConstantsRepo constantsRepo) {
		ROOF_SUPPORT_MAX_DISTANCE = constantsRepo.getWorldConstants().getRoofSupportMaxDistance();
	}

	public void roofConstructionAdded(MapTile mapTile) {
		switchState(mapTile, RoofConstructionState.PENDING);

		if (!withinRangeOfSupport(mapTile)) {
			switchState(mapTile, TOO_FAR_FROM_SUPPORT);
		} else {
			// is in range of support
			if (hasAdjacentRoof(mapTile) || containsSupport(mapTile)) {
				switchState(mapTile, RoofConstructionState.READY_FOR_CONSTRUCTION);
			} else {
				switchState(mapTile, RoofConstructionState.NO_ADJACENT_ROOF);
			}
		}
	}

	public void roofConstructionRemoved(MapTile mapTile) {
		// cancel any outstanding jobs
		if (mapTile.getRoof().getConstructionState().equals(RoofConstructionState.READY_FOR_CONSTRUCTION)) {
			Logger.warn("TODO: cancel pending construction");
		}

		mapTile.getRoof().setConstructionState(RoofConstructionState.NONE);
	}

	public void roofConstructed(MapTile mapTile) {
		for (CompassDirection direction : CompassDirection.CARDINAL_DIRECTIONS) {
			MapTile tile = gameContext.getAreaMap().getTile(mapTile.getTileX() + direction.getXOffset(), mapTile.getTileY() + direction.getYOffset());
			if (tile != null && tile.getRoof().getConstructionState().equals(NO_ADJACENT_ROOF)) {
				switchState(tile, READY_FOR_CONSTRUCTION);
			}
		}
	}

	public void supportConstructed(MapTile mapTile) {
		for (int yCursor = mapTile.getTileY()-ROOF_SUPPORT_MAX_DISTANCE; yCursor <= mapTile.getTileY()+ROOF_SUPPORT_MAX_DISTANCE; yCursor++) {
			for (int xCursor = mapTile.getTileX()-ROOF_SUPPORT_MAX_DISTANCE; xCursor <= mapTile.getTileX()+ROOF_SUPPORT_MAX_DISTANCE; xCursor++) {
				MapTile tile = gameContext.getAreaMap().getTile(xCursor, yCursor);
				if (tile != null && tile.getRoof().getState().equals(TileRoofState.OPEN) && tile.getRoof().getConstructionState().equals(TOO_FAR_FROM_SUPPORT)) {
					switchState(tile, NO_ADJACENT_ROOF);
					if (hasAdjacentRoof(tile) || (yCursor == mapTile.getTileY() && xCursor == mapTile.getTileX())) {
						switchState(tile, READY_FOR_CONSTRUCTION);
					}
				}
			}
		}
	}

	public void roofDeconstructed(MapTile mapTile) {


		// need to check there are no not-attached sections of roofing
	}

	public void supportDeconstructed(MapTile mapTile) {

	}

	private void switchState(MapTile mapTile, RoofConstructionState newState) {
		mapTile.getRoof().setConstructionState(newState);

		if (newState.equals(RoofConstructionState.READY_FOR_CONSTRUCTION)) {
			Logger.warn("TODO: create roof construction job");
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private boolean hasAdjacentRoof(MapTile mapTile) {
		for (CompassDirection direction : CompassDirection.CARDINAL_DIRECTIONS) {
			MapTile tile = gameContext.getAreaMap().getTile(mapTile.getTileX() + direction.getXOffset(), mapTile.getTileY() + direction.getYOffset());
			if (tile != null && !tile.getRoof().getState().equals(TileRoofState.OPEN)) {
				return true;
			}
		}
		return false;
	}

	private boolean withinRangeOfSupport(MapTile mapTile) {
		for (int yCursor = mapTile.getTileY()-ROOF_SUPPORT_MAX_DISTANCE; yCursor <= mapTile.getTileY()+ROOF_SUPPORT_MAX_DISTANCE; yCursor++) {
			for (int xCursor = mapTile.getTileX()-ROOF_SUPPORT_MAX_DISTANCE; xCursor <= mapTile.getTileX()+ROOF_SUPPORT_MAX_DISTANCE; xCursor++) {
				MapTile tile = gameContext.getAreaMap().getTile(xCursor, yCursor);
				if (tile != null && (tile.hasWall() || containsSupport(tile))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean containsSupport(MapTile tile) {
		return tile.getEntities().stream().anyMatch(e -> e.getTag(SupportsRoofTag.class) != null);
	}

}
