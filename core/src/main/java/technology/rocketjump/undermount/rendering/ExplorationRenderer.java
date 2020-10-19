package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.mapping.tile.TileExploration;
import technology.rocketjump.undermount.rendering.custom_libgdx.VertexColorOnlyBatch;
import technology.rocketjump.undermount.rendering.utils.ColorMixer;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.List;

import static technology.rocketjump.undermount.rendering.RenderMode.DIFFUSE;

public class ExplorationRenderer implements Disposable {

	private static final Color highColor = HexColors.get("#e5d0bb");
	private static final Color lowColor = HexColors.get("#534c47");
	private final VertexColorOnlyBatch vertexColorBatch;

	@Inject
	public ExplorationRenderer() {
		vertexColorBatch = new VertexColorOnlyBatch();
	}

	public void render(List<MapTile> unexploredTiles, OrthographicCamera camera, TiledMap tiledMap, RenderMode renderMode) {
		if (!renderMode.equals(DIFFUSE)) {
			return;
		}


		vertexColorBatch.setProjectionMatrix(camera.combined);
		vertexColorBatch.begin();
		for (MapTile tile : unexploredTiles) {
			if (tile.getExploration().equals(TileExploration.UNEXPLORED)) {
				renderUnexplored(tile, tiledMap);
			} else {
				renderPartiallyExplored(tile, tiledMap);
			}
		}
		vertexColorBatch.end();
	}

	private void renderUnexplored(MapTile tile, TiledMap tiledMap) {
		// lower left, upper left, upper right, lower right
		Color[] vertexColors = new Color[] {
				ColorMixer.interpolate(0, 1, tiledMap.getVertex(tile, CompassDirection.SOUTH_WEST).getHeightmapValue(), lowColor, highColor),
				ColorMixer.interpolate(0, 1, tiledMap.getVertex(tile, CompassDirection.NORTH_WEST).getHeightmapValue(), lowColor, highColor),
				ColorMixer.interpolate(0, 1, tiledMap.getVertex(tile, CompassDirection.NORTH_EAST).getHeightmapValue(), lowColor, highColor),
				ColorMixer.interpolate(0, 1, tiledMap.getVertex(tile, CompassDirection.SOUTH_EAST).getHeightmapValue(), lowColor, highColor)
		};
		vertexColorBatch.draw(tile.getTileX(), tile.getTileY(), 1, 1, vertexColors);
	}

	/**
	 * This method renders a tile as 4 quadrants so blend to alpha is smoother
	 */
	private void renderPartiallyExplored(MapTile tile, TiledMap tiledMap) {
		MapVertex swVert = tiledMap.getVertex(tile, CompassDirection.SOUTH_WEST);
		MapVertex nwVert = tiledMap.getVertex(tile, CompassDirection.NORTH_WEST);
		MapVertex neVert = tiledMap.getVertex(tile, CompassDirection.NORTH_EAST);
		MapVertex seVert = tiledMap.getVertex(tile, CompassDirection.SOUTH_EAST);

		float middleHeight = (swVert.getHeightmapValue() + nwVert.getHeightmapValue() + neVert.getHeightmapValue() + seVert.getHeightmapValue()) / 4f;
		float middleAlpha = (swVert.getExplorationVisibility() + nwVert.getExplorationVisibility() + neVert.getExplorationVisibility() + seVert.getExplorationVisibility()) / 4f;

		Color[] vertexColors;
		// Lower left quadrant
		vertexColors = new Color[] {
				ColorMixer.interpolate(0, 1, swVert.getHeightmapValue(), lowColor, highColor),
				ColorMixer.interpolate(0, 1, (swVert.getHeightmapValue() + nwVert.getHeightmapValue()) / 2f, lowColor, highColor),
				ColorMixer.interpolate(0, 1, middleHeight, lowColor, highColor),
				ColorMixer.interpolate(0, 1, (swVert.getHeightmapValue() + seVert.getHeightmapValue()) / 2f, lowColor, highColor)
		};
		vertexColors[0].a = 1f - swVert.getExplorationVisibility();
		vertexColors[1].a = 1f - (swVert.getExplorationVisibility() + nwVert.getExplorationVisibility()) / 2f;
		vertexColors[2].a = 1f - middleAlpha;
		vertexColors[3].a = 1f - (swVert.getExplorationVisibility() + seVert.getExplorationVisibility()) / 2f;
		vertexColorBatch.draw(tile.getTileX(), tile.getTileY(), 0.5f, 0.5f, vertexColors);

		// Upper left quadrant
		vertexColors = new Color[] {
				ColorMixer.interpolate(0, 1, (swVert.getHeightmapValue() + nwVert.getHeightmapValue()) / 2f, lowColor, highColor),
				ColorMixer.interpolate(0, 1, nwVert.getHeightmapValue(), lowColor, highColor),
				ColorMixer.interpolate(0, 1, (nwVert.getHeightmapValue() + neVert.getHeightmapValue()) / 2f, lowColor, highColor),
				ColorMixer.interpolate(0, 1, middleHeight, lowColor, highColor)
		};
		vertexColors[0].a = 1f - (swVert.getExplorationVisibility() + nwVert.getExplorationVisibility()) / 2f;
		vertexColors[1].a = 1f - nwVert.getExplorationVisibility();
		vertexColors[2].a = 1f - (nwVert.getExplorationVisibility() + neVert.getExplorationVisibility()) / 2f;
		vertexColors[3].a = 1f - middleAlpha;
		vertexColorBatch.draw(tile.getTileX(), tile.getTileY() + 0.5f, 0.5f, 0.5f, vertexColors);

		// Upper right quadrant
		vertexColors = new Color[] {
				ColorMixer.interpolate(0, 1, middleHeight, lowColor, highColor),
				ColorMixer.interpolate(0, 1, (nwVert.getHeightmapValue() + neVert.getHeightmapValue()) / 2f, lowColor, highColor),
				ColorMixer.interpolate(0, 1, neVert.getHeightmapValue(), lowColor, highColor),
				ColorMixer.interpolate(0, 1, (neVert.getHeightmapValue() + seVert.getHeightmapValue()) / 2f, lowColor, highColor)
		};
		vertexColors[0].a = 1f - middleAlpha;
		vertexColors[1].a = 1f - (nwVert.getExplorationVisibility() + neVert.getExplorationVisibility()) / 2f;
		vertexColors[2].a = 1f - neVert.getExplorationVisibility();
		vertexColors[3].a = 1f - (neVert.getExplorationVisibility() + seVert.getExplorationVisibility()) / 2f;
		vertexColorBatch.draw(tile.getTileX() + 0.5f, tile.getTileY() + 0.5f, 0.5f, 0.5f, vertexColors);

		// Lower right quadrant
		vertexColors = new Color[] {
				ColorMixer.interpolate(0, 1, (swVert.getHeightmapValue() + seVert.getHeightmapValue()) / 2f, lowColor, highColor),
				ColorMixer.interpolate(0, 1, middleHeight, lowColor, highColor),
				ColorMixer.interpolate(0, 1, (neVert.getHeightmapValue() + seVert.getHeightmapValue()) / 2f, lowColor, highColor),
				ColorMixer.interpolate(0, 1, seVert.getHeightmapValue(), lowColor, highColor)
		};
		vertexColors[0].a = 1f - (swVert.getExplorationVisibility() + seVert.getExplorationVisibility()) / 2f;
		vertexColors[1].a = 1f - middleAlpha;
		vertexColors[2].a = 1f - (neVert.getExplorationVisibility() + seVert.getExplorationVisibility()) / 2f;
		vertexColors[3].a = 1f - seVert.getExplorationVisibility();
		vertexColorBatch.draw(tile.getTileX() + 0.5f, tile.getTileY(), 0.5f, 0.5f, vertexColors);
	}

	@Override
	public void dispose() {
		vertexColorBatch.dispose();
	}
}
