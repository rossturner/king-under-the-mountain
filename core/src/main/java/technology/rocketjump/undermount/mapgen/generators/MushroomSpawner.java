package technology.rocketjump.undermount.mapgen.generators;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.undermount.mapgen.model.input.GameMapGenerationParams;
import technology.rocketjump.undermount.mapgen.model.input.MushroomType;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.GameMapTile;
import technology.rocketjump.undermount.mapgen.model.output.MapSubRegion;
import technology.rocketjump.undermount.mapgen.model.output.TileSubType;

import java.util.*;

import static technology.rocketjump.undermount.mapgen.model.output.TileSubType.LOAMY_FLOOR_CAVE;
import static technology.rocketjump.undermount.mapgen.model.output.TileSubType.STONE_FLOOR_CAVE;

public class MushroomSpawner {

	private static final int NUM_LOAMY_FLOOR_SURROUNDING_MUSHROOMS = 14;
	private final List<MushroomType> mushroomTypes;
	private float totalWeightings;

	public MushroomSpawner(List<MushroomType> mushroomTypes) {
		this.mushroomTypes = mushroomTypes;
		this.totalWeightings = 0f;
		for (MushroomType mushroomType : mushroomTypes) {
			this.totalWeightings += mushroomType.getWeighting();
		}
	}

	public void placeMushrooms(GameMap map, Random random, GameMapGenerationParams generationParams) {
		List<GameMapTile> allCaveTiles = new ArrayList<>();

		for (MapSubRegion subRegion : map.getSubRegions().values()) {
			if (subRegion.getSubRegionType().equals(STONE_FLOOR_CAVE)) {
				allCaveTiles.addAll(subRegion.getTiles());
			}
		}

		List<MushroomType> types = generationParams.getMushroomTypes();
		if (types.isEmpty()) {
			return;
		}

		Collections.shuffle(allCaveTiles, random);

		int totalMushroomsRequired = (int)(((float)allCaveTiles.size()) * generationParams.getRequiredMushroomRatio());
		int numMushroomsToPlaceRandomly = (int)((float)totalMushroomsRequired / 2.5f);
		List<GridPoint2> mushroomLocations = new ArrayList<>();
		int numMushroomsToPlaceNearOthers = totalMushroomsRequired - numMushroomsToPlaceRandomly;

		while (numMushroomsToPlaceRandomly > 0 && allCaveTiles.size() > 0) {
			GameMapTile mushroomTile = allCaveTiles.remove(0);
			mushroomTile.setMushroom(pickMushroomType(random));
			numMushroomsToPlaceRandomly--;
			mushroomLocations.add(mushroomTile.getPosition());
			mushroomTile.setTileSubType(TileSubType.LOAMY_FLOOR_CAVE);
			List<GameMapTile> loamyTiles = new ArrayList<>();

			for (GameMapTile neighbourTile : map.getAllNeighbourTiles(mushroomTile.getPosition().x, mushroomTile.getPosition().y)) {
				if (neighbourTile.getTileSubType().equals(STONE_FLOOR_CAVE)) {
					neighbourTile.setTileSubType(LOAMY_FLOOR_CAVE);
					allCaveTiles.remove(neighbourTile);
					loamyTiles.add(neighbourTile);
				}
			}

			Collections.shuffle(loamyTiles, random);

			for (int i = 0; i < 3; i++) {
				if (loamyTiles.size() < NUM_LOAMY_FLOOR_SURROUNDING_MUSHROOMS) {
					addMoreLoam(loamyTiles, allCaveTiles, map, random);
				}
			}

		}

		while (numMushroomsToPlaceNearOthers > 0 && !mushroomLocations.isEmpty()) {
			GridPoint2 location = mushroomLocations.get(random.nextInt(mushroomLocations.size()));

			int attempts = 0;
			while (attempts < 5) {
				GridPoint2 nearbyLocation = location.cpy();
				if (random.nextBoolean()) {
					// East/west offset
					if (random.nextBoolean()) {
						nearbyLocation.add(2, 0);
					} else {
						nearbyLocation.add(-2, 0);
					}
					nearbyLocation.add(0, -2 + random.nextInt(5));
				} else {
					if (random.nextBoolean()) {
						nearbyLocation.add(0, 2);
					} else {
						nearbyLocation.add(0, -2);
					}
					nearbyLocation.add(-2 + random.nextInt(5), 0);
				}

				GameMapTile nearbyTile = map.get(nearbyLocation);
				if (nearbyTile != null && nearbyTile.getTileSubType().equals(LOAMY_FLOOR_CAVE) && !nearbyTile.hasMushroom()) {
					boolean adjacentMushroom = false;

					for (GameMapTile gameMapTile : map.getAllNeighbourTiles(nearbyLocation.x, nearbyLocation.y)) {
						if (gameMapTile.hasMushroom()) {
							adjacentMushroom = true;
							break;
						}
					}

					if (!adjacentMushroom) {
						nearbyTile.setMushroom(pickMushroomType(random));
						numMushroomsToPlaceNearOthers--;
						mushroomLocations.add(nearbyLocation);
						break;
					}
				}

				attempts++;
			}
		}
	}

	private MushroomType pickMushroomType(Random random) {
		float cursor = random.nextFloat() * totalWeightings;
		for (MushroomType mushroomType : mushroomTypes) {
			cursor -= mushroomType.getWeighting();
			if (cursor <= 0) {
				return mushroomType;
			}
		}
		// This shouldn't happen?
		return mushroomTypes.get(random.nextInt(mushroomTypes.size()));
	}

	private void addMoreLoam(List<GameMapTile> loamyTiles, List<GameMapTile> allCaveTiles, GameMap map, Random random) {
		Iterator<GameMapTile> neighbourIterator = loamyTiles.iterator();
		List<GameMapTile> moreLoamyTiles = new ArrayList<>();
		while (neighbourIterator.hasNext() && (loamyTiles.size() + moreLoamyTiles.size()) < NUM_LOAMY_FLOOR_SURROUNDING_MUSHROOMS) {
			GameMapTile neighbourTile = neighbourIterator.next();
			List<GameMapTile> moreTiles = map.getOrthogonalNeighbours(neighbourTile.getPosition());
			Collections.shuffle(moreTiles, random);
			for (GameMapTile furtherAwayTile : moreTiles) {
				if (furtherAwayTile.getTileSubType().equals(STONE_FLOOR_CAVE)) {
					furtherAwayTile.setTileSubType(LOAMY_FLOOR_CAVE);
					allCaveTiles.remove(furtherAwayTile);
					moreLoamyTiles.add(furtherAwayTile);
				}
			}
		}
		loamyTiles.addAll(moreLoamyTiles);
	}
}
