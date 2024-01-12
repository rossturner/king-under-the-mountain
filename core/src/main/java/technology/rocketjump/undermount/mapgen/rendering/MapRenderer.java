package technology.rocketjump.undermount.mapgen.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.mapgen.generators.TreePlanter;
import technology.rocketjump.undermount.mapgen.model.*;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.GameMapTile;
import technology.rocketjump.undermount.mapgen.model.output.TileSubType;

public class MapRenderer {

	private static final Color TREE_BARK_COLOR = new Color(102f/255f, 51f/255f, 0f, 1f);
	private static final Color TREE_LEAF_COLOR = new Color(93f/255f, 213f/255f, 93f/255f, 1f);
	private static final Color SHRUB_COLOR = new Color(91f/255f, 179f/255f, 62f/255f, 1f);
	private ShapeRenderer shapeRenderer;
	private OrthographicCamera camera = new OrthographicCamera();

	public MapRenderer() {
		shapeRenderer = new ShapeRenderer();
	}

	public <T extends AbstractGameMap> void render(T map) {
		if (map instanceof VertexGameMap) {
			renderVertexMap((VertexGameMap)map);
		} else if (map instanceof HeightGameMap) {
			renderHeightMap((HeightGameMap)map);
		} else if (map instanceof GameMap) {
			renderGameMap((GameMap)map);
		}
	}

	public void renderGameMap(GameMap gameMap) {
		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();
		camera.setToOrtho(false, screenWidth, screenHeight);

		int mapWidth = gameMap.getWidth();
		int mapHeight = gameMap.getHeight();

		float widthScaling = ((float)screenWidth) / ((float)mapWidth);
		float heightScaling = (float)screenHeight / (float)mapHeight;

		float tileSize = heightScaling;
		if (heightScaling * mapWidth > screenWidth) {
			// Height scaling would go off screen
			tileSize = widthScaling;
		}

		float mapWidthPixels = tileSize * mapWidth;
		float mapHeightPixels = tileSize * mapHeight;

		float originX = (screenWidth / 2) - (mapWidthPixels / 2);
		float originY = (screenHeight / 2) - (mapHeightPixels / 2);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setProjectionMatrix(camera.combined);
		for (int y = mapHeight - 1; y >= 0; y--) {
			for (int x = 0; x < mapWidth; x++) {

				GameMapTile tile = gameMap.get(x, y);

				if (tile.hasTree()) {
					// Special case for tree - covers multiple tiles
					shapeRenderer.setColor(TREE_BARK_COLOR);
					shapeRenderer.rect(originX + (x * tileSize), originY + (y * tileSize), tileSize, tileSize * TreePlanter.MAX_TREE_HEIGHT_TILES);
					shapeRenderer.setColor(TREE_LEAF_COLOR);
					// return ; // light brown dirt / taiga
					shapeRenderer.circle(originX + (x * tileSize) + (tileSize / 2f), originY + (y * tileSize) + (tileSize * 3.5f), tileSize * 1.5f);
				} else if (tile.hasShrub()) {
					shapeRenderer.setColor(SHRUB_COLOR);
					if (tile.getShrubType().hasFruit()) {
						shapeRenderer.setColor(SHRUB_COLOR.cpy().add(0.1f, 0.1f, 0.1f, 0));
					}
					shapeRenderer.circle(originX + (x * tileSize) + (tileSize / 2f), originY + (y * tileSize) + (tileSize / 2f), tileSize * 0.5f);
				} else {
					shapeRenderer.setColor(toColor(tile, gameMap));
					shapeRenderer.rect(originX + (x * tileSize), originY + (y * tileSize), tileSize, tileSize);
				}

			}
		}
		shapeRenderer.end();

	}

	public void renderHeightMap(HeightGameMap heightMap) {
		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();
		camera.setToOrtho(false, screenWidth, screenHeight);

		int mapWidth = heightMap.getWidth();
		int mapHeight = heightMap.getHeight();

		float widthScaling = ((float)screenWidth) / ((float)mapWidth);
		float heightScaling = (float)screenHeight / (float)mapHeight;

		float tileSize = heightScaling;
		if (heightScaling * mapWidth > screenWidth) {
			// Height scaling would go off screen
			tileSize = widthScaling;
		}

		float mapWidthPixels = tileSize * mapWidth;
		float mapHeightPixels = tileSize * mapHeight;

		float originX = (screenWidth / 2) - (mapWidthPixels / 2);
		float originY = (screenHeight / 2) - (mapHeightPixels / 2);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setProjectionMatrix(camera.combined);
		for (int y = mapHeight - 1; y >= 0; y--) {
			for (int x = 0; x < mapWidth; x++) {

				float heightmapValue = heightMap.getHeight(x, y);

				shapeRenderer.setColor(toColor(heightmapValue));
				shapeRenderer.rect(originX + (x * tileSize), originY + (y * tileSize), tileSize, tileSize);
			}
		}
//		shapeRenderer.setColor(Color.GREEN);
//		shapeRenderer.rect(10f, 10, 60f, 40f);
		shapeRenderer.end();


	}

	public void renderVertexMap(VertexGameMap vertexMap) {
		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();
		camera.setToOrtho(false, screenWidth, screenHeight);

		int mapWidth = vertexMap.getNumTilesWide();
		int mapHeight = vertexMap.getNumTilesHigh();

		float widthScaling = ((float)screenWidth) / ((float)mapWidth);
		float heightScaling = (float)screenHeight / (float)mapHeight;

		float tileSize = heightScaling;
		if (heightScaling * mapWidth > screenWidth) {
			// Height scaling would go off screen
			tileSize = widthScaling;
		}

		float originX = (screenWidth / 2) - (tileSize * mapWidth / 2);
		float originY = (screenHeight / 2) - (tileSize * mapHeight / 2);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setProjectionMatrix(camera.combined);
		for (int y = mapHeight - 1; y >= 0; y--) {
			for (int x = 0; x < mapWidth; x++) {


				float heightmapX0Y0 = vertexMap.get(x, y).getHeight();
				float heightmapX1Y0 = vertexMap.get(x + 1, y).getHeight();
				float heightmapX0Y1 = vertexMap.get(x, y + 1).getHeight();
				float heightmapX1Y1 = vertexMap.get(x + 1, y + 1).getHeight();
				float averageHeight = (heightmapX0Y0 + heightmapX0Y1 + heightmapX1Y0 + heightmapX1Y1) / 4f;

				// TODO just track aX bX and aY bY
				Vector2 lowerLeftPoint = new Vector2(originX + (x * tileSize), originY + (y * tileSize));

				shapeRenderer.setColor(toColor(averageHeight));
				shapeRenderer.rect(lowerLeftPoint.x, lowerLeftPoint.y, tileSize, tileSize);
			}
		}
		shapeRenderer.end();
	}

	private Color toColor(GameMapTile tile, GameMap gameMap) {
//		if (tile.getSubRegion() != null && tile.getSubRegion().getSubRegionId() == regionHighlight) {
//			return Color.WHITE;
//		} else if (tile.getRegion() != null && tile.getRegion().getRegionId() == regionHighlight) {
//			return Color.WHITE;
//		}

		if (tile.hasRiver()) {
			if (gameMap.getRiverStartTiles().contains(tile.getPosition())) {
//				return Color.WHITE;
				return Color.BLUE.cpy().add(0.2f, 0.2f, 0, 1);
			} else if (gameMap.getRiverEndTiles().contains(tile.getPosition())) {
//				return Color.YELLOW;
				return Color.BLUE.cpy().mul(0.7f);
			} else {
				return Color.BLUE;
			}
		} else if (tile.getFloorType() == FloorType.Outdoor) {
//			return new Color(tile.getHeightMapValue(), tile.getHeightMapValue() + 0.3f, tile.getHeightMapValue(), 1f);
			return toBiomeColor(tile);
//			return new Color(51f/255, 153f/255f, 51f/255f, 1f);
		} else if (tile.getFloorType() == FloorType.Rock) {
			if (tile.hasMushroom()) {
				return Color.PURPLE;
			} else if (tile.getTileSubType().equals(TileSubType.LOAMY_FLOOR_CAVE)) {
				return Color.BROWN;
			} else if (tile.getRockType() != null) {
				return tile.getRockType().getColor().cpy().mul(0.5f);
			} else if (!tile.getRockGroup().equals(RockGroup.None)) {
				return tile.getRockGroup().getColor().cpy().mul(0.5f);
			} else {
				return Color.GRAY;
			}
		} else if (tile.getGem() != null) {
			return tile.getGem().getColor();
		} else if (tile.hasOre()) {
			return tile.getOre().getOreColor();
		} else {
			// This is a mountain tile
			if (tile.getRockType() != null) {
				return tile.getRockType().getColor();
			} else if (!tile.getRockGroup().equals(RockGroup.None)) {
				return tile.getRockGroup().getColor();
			} else {
				return Color.LIGHT_GRAY;
			}
		}
	}

	private Color toBiomeColor(GameMapTile tile) {
		if (tile.getTileSubType() == null) {
			return toColor(tile.getNoisyHeightValue());
		} else if (tile.getTileSubType().equals(TileSubType.FOREST)) {
			return new Color(47f / 255f, 142f / 255f, 47f/255f, 1f); // dark green forest
		} else if (tile.getTileSubType().equals(TileSubType.GRASSLAND)) {
			return new Color(42f/255f, 162f/255f, 42f/255f, 1f); // grass and shrubbery
		} else if (tile.getTileSubType().equals(TileSubType.PLAINS)) {
			return new Color(52f/255, 203f/255f, 52f/255f, 1f); // Plains, grass and dirt but no trees
		} else {
			return new Color(93f/255f, 213f/255f, 93f/255f, 1f); // light brown dirt / taiga
		}
	}

	private Color toColor(float height) {
		return new Color(height, height, height, 1);
	}
}
