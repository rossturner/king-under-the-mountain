package technology.rocketjump.undermount.mapping.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.floor.RiverTile;

import java.util.*;

import static technology.rocketjump.undermount.mapping.tile.CompassDirection.*;

/**
 * WARNING This class is not at all thread-safe
 *
 * Also the river does not always extend across the map despite a few hacks/tweaks, usually when there are one or two narrow gaps for the
 * river to fit through. This might best be tackled by applying in-game water flow to those water source tiles which border a "channel"
 * that does not yet have the river in it
 */
public class WaterFlowCalculator {

	public static final float CHANCE_SINGLE_WATER_EVAPORATES = 0.05f;

	private Random random;
	private int stepCounter;
	private int numStepsSinceLastUpdate;
	private List<FlowTransition> transitionsToApply;
	private List<GridPoint2> activeWaterTiles;
	private List<GridPoint2> riverStartTiles;
	private List<GridPoint2> riverEndTiles;
	private Map<GridPoint2, Boolean> riverTiles;

	public void calculateRiverFlow(GameMap sourceMap, TiledMap targetMap, long seed) {
		if (sourceMap.getRiverStartTiles().isEmpty()) {
			Logger.info("No river in generated map, skipping river flow (should probably force-regenerate map until other water sources are available)");
			return;
		}

		Logger.info("Running river...");
		random = new RandomXS128(seed);
		stepCounter = 0;
		numStepsSinceLastUpdate = 0;
		transitionsToApply = new ArrayList<>();

		activeWaterTiles = new ArrayList<>();
		riverStartTiles = sourceMap.getRiverStartTiles();
		riverEndTiles = sourceMap.getRiverEndTiles();

		riverTiles = new HashMap<>();
		for (GridPoint2 riverTileLocation : sourceMap.getRiverTiles()) {
			riverTiles.put(riverTileLocation, true);
		}


		for (GridPoint2 startLocation : sourceMap.getRiverStartTiles()) {
			MapTile startTile = targetMap.getTile(startLocation);
			startTile.getFloor().setRiverTile(new RiverTile(true));
			activeWaterTiles.add(startLocation);
		}

		boolean riverFinished = false;
		while (!riverFinished) {
			riverFinished = processStep(targetMap);
		}
	}

	public boolean processStep(TiledMap map) {

		List<GridPoint2> waterTileToUpdateThisStep = new ArrayList<>(activeWaterTiles);

		if (waterTileToUpdateThisStep.size() == 0) {
			Logger.info("No more steps to process, completed " + stepCounter + " steps");
			return true;
		}

		transitionsToApply.clear();

		stepCounter++;
		for (GridPoint2 cursor : waterTileToUpdateThisStep) {
			processTile(map, cursor);
		}

		for (FlowTransition flowTransition : transitionsToApply) {
			flowTransition.source.getFloor().getRiverTile().decrementWater(flowTransition.flowDirection, random);
			if (flowTransition.source.getFloor().getRiverTile().getWaterAmount() == 0 && random.nextFloat() < CHANCE_SINGLE_WATER_EVAPORATES) {
				continue;
			}
			if (flowTransition.target != null) {
				flowTransition.target.getFloor().getRiverTile().incrementWater(flowTransition.flowDirection);
			}
		}
		if (transitionsToApply.isEmpty()) {
			numStepsSinceLastUpdate++;
			// No transitions to apply but still have active tiles - something went wrong with propagation
			if (numStepsSinceLastUpdate > RiverTile.INACTIVITY_THRESHOLD) {
				// Just going to force max water for now
				for (GridPoint2 cursor : waterTileToUpdateThisStep) {
					RiverTile riverTile = map.getTile(cursor).getFloor().getRiverTile();
					riverTile.setWaterAmount(RiverTile.MAX_WATER_PER_TILE);
					riverTile.inactive();
				}
				return true;
			}
		} else {
			numStepsSinceLastUpdate = 0;
		}

		return false;
	}

	private void processTile(TiledMap map, GridPoint2 cursorPosition) {
		MapTile cursorTile = map.getTile(cursorPosition);
		RiverTile cursorRiverTile = cursorTile.getFloor().getRiverTile();
		int cursorTileWaterAmount = cursorRiverTile.getWaterAmount();
		boolean surroundedByMaxWater = true;

		List<CompassDirection> randomisedDirections = new ArrayList<>();
		randomisedDirections.addAll(CompassDirection.CARDINAL_DIRECTIONS);
		Collections.shuffle(randomisedDirections, random);
		boolean transitionFound = false;

		// Check to see if river is one-tile wide at this point, and if so, make this a source tile
		if (!cursorRiverTile.isSourceTile()) {
			MapTile north = map.getTile(cursorPosition.cpy().add(NORTH.getXOffset(), NORTH.getYOffset()));
			MapTile south = map.getTile(cursorPosition.cpy().add(SOUTH.getXOffset(), SOUTH.getYOffset()));
			MapTile east = map.getTile(cursorPosition.cpy().add(EAST.getXOffset(), EAST.getYOffset()));
			MapTile west = map.getTile(cursorPosition.cpy().add(WEST.getXOffset(), WEST.getYOffset()));

			if ( (!shouldBeRiver(north) && !shouldBeRiver(south)) ||
					(!shouldBeRiver(east) && !shouldBeRiver(west)) ) {
				cursorRiverTile.setIsSourceTile(true);
			}
		}


		for (CompassDirection directionToTry : randomisedDirections) {
			MapTile tileInDirection = map.getTile(cursorPosition.cpy().add(directionToTry.getXOffset(), directionToTry.getYOffset()));


			if (tileInDirection == null && !cursorRiverTile.isSourceTile()) {
				// Only flow out on river ends
				if (isRiverEnd(cursorPosition) && random.nextFloat() < 0.1f && cursorTileWaterAmount > 1) {
					transitionsToApply.add(new FlowTransition(cursorTile, null, directionToTry));
					transitionFound = true;
				}
			} else if (tileInDirection != null && shouldBeRiver(tileInDirection)) {
				// Potential water tile to move to
				RiverTile riverTileInDirection = tileInDirection.getFloor().getRiverTile();
				int waterAmountInDirection = 0;
				if (riverTileInDirection != null) {
					waterAmountInDirection = riverTileInDirection.getWaterAmount();
				}

				if (waterAmountInDirection != RiverTile.MAX_WATER_PER_TILE) {
					surroundedByMaxWater = false;
				}

				// Only one water transition per tile for non-source tiles - this is needed for when only one source tile is exposed, rest are trapped
				if (!transitionFound || cursorRiverTile.isSourceTile()) {
					if (waterAmountInDirection < cursorTileWaterAmount) {

						if (riverTileInDirection == null) {
							riverTileInDirection = new RiverTile(false);
							tileInDirection.getFloor().setRiverTile(riverTileInDirection);
							activeWaterTiles.add(tileInDirection.getTilePosition());
						}

						transitionsToApply.add(new FlowTransition(cursorTile, tileInDirection, directionToTry));

						if (waterAmountInDirection < cursorTileWaterAmount / 2) {
							// Double move for more than twice different
							transitionsToApply.add(new FlowTransition(cursorTile, tileInDirection, directionToTry));
						}
						transitionFound = true;
					}
				}

			}

		}

		if (surroundedByMaxWater && cursorTileWaterAmount == RiverTile.MAX_WATER_PER_TILE && !transitionFound) {
			cursorRiverTile.inactive();
		} else {
			cursorRiverTile.active();
		}

		if (!cursorRiverTile.isActive()) {
			activeWaterTiles.remove(cursorPosition);
			if (cursorRiverTile.isSourceTile() && !isRiverStart(cursorPosition)) {
				cursorRiverTile.setIsSourceTile(false); // Removing fudge which set some tiles to be source tiles
			}
		}
	}

	private boolean isRiverStart(GridPoint2 cursorPosition) {
		return riverStartTiles.contains(cursorPosition);
	}

	private boolean shouldBeRiver(MapTile tileInDirection) {
		if (tileInDirection == null) {
			return false;
		} else {
			return riverTiles.containsKey(tileInDirection.getTilePosition());
		}
	}

	private boolean isRiverEnd(GridPoint2 cursorPosition) {
		return riverEndTiles.contains(cursorPosition);
	}


	public static class FlowTransition {

		public final MapTile source;
		public final MapTile target;
		public final CompassDirection flowDirection;

		public FlowTransition(MapTile source, MapTile target, CompassDirection flowDirection) {
			this.source = source;
			this.target = target;
			this.flowDirection = flowDirection;
		}
	}

}
