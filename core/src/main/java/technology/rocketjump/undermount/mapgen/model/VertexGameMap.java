package technology.rocketjump.undermount.mapgen.model;

import com.badlogic.gdx.utils.Array;

public class VertexGameMap extends AbstractGameMap {

	private final int numTilesWide, numTilesHigh;

	private Array<Array<HeightmapVertex>> heightMap;

	public VertexGameMap(int numTilesWide, int numTilesHigh) {
		super(numTilesWide, numTilesHigh);
		this.numTilesWide = numTilesWide;
		this.numTilesHigh = numTilesHigh;

		heightMap = new Array<>(numTilesWide + 1);
		for (int x = 0; x <= numTilesWide; x++) {
			Array<HeightmapVertex> yArray = new Array<>(numTilesHigh + 1);
			for (int y = 0; y <= numTilesHigh; y++) {
				yArray.add(new HeightmapVertex());
			}
			heightMap.add(yArray);
		}
	}

	public int getNumTilesWide() {
		return numTilesWide;
	}

	public int getNumTilesHigh() {
		return numTilesHigh;
	}

	public HeightmapVertex get(int x, int y) {
		if (x < 0 || x > numTilesWide) {
			return null;
		} else if (y < 0 || y > numTilesHigh) {
			return null;
		} else {
			return heightMap.get(x).get(y);
		}
	}

	public void set(int x, int y, float height) {
		if (x < 0 || x > numTilesWide) {
			// Do nothing
		} else if (y < 0 || y > numTilesHigh) {
			// Do nothing
		} else {
			heightMap.get(x).get(y).setHeight(height);
		}
	}

	/**
	 * This method normalises everything to be between 0 and 1
	 */
	public void normalise() {
		float minHeight = Float.MAX_VALUE;
		float maxHeight = Float.MIN_VALUE;

		for (int x = 0; x <= numTilesWide; x++) {
			for (int y = 0; y <= numTilesHigh; y++) {
				float heightAtPoint = get(x, y).getHeight();
				if (heightAtPoint < minHeight) {
					minHeight = heightAtPoint;
				}
				if (heightAtPoint > maxHeight) {
					maxHeight = heightAtPoint;
				}
			}
		}

		for (int x = 0; x <= numTilesWide; x++) {
			for (int y = 0; y <= numTilesHigh; y++) {
				float heightAtPoint = get(x, y).getHeight();

				float adjusted = (heightAtPoint - minHeight) / (maxHeight - minHeight);
				set(x, y, adjusted);
			}
		}
	}

	public void setAverageOfDiamond(int x, int y) {
		float totalHeightNeighbours = 0f;
		float numNeigbours = 0f;

		HeightmapVertex neighbour;
		neighbour = get(x - 1, y);
		if (neighbour != null) {
			totalHeightNeighbours += neighbour.getHeight();
			numNeigbours++;
		}
		neighbour = get(x + 1, y);
		if (neighbour != null) {
			totalHeightNeighbours += neighbour.getHeight();
			numNeigbours++;
		}
		neighbour = get(x, y - 1);
		if (neighbour != null) {
			totalHeightNeighbours += neighbour.getHeight();
			numNeigbours++;
		}
		neighbour = get(x, y + 1);
		if (neighbour != null) {
			totalHeightNeighbours += neighbour.getHeight();
			numNeigbours++;
		}

		float average = totalHeightNeighbours / numNeigbours;
		set(x, y, average);
	}

	public void setAverageOfSquare(int x, int y) {
		float totalHeightNeighbours = 0f;
		float numNeigbours = 0f;

		HeightmapVertex neighbour;
		neighbour = get(x - 1, y - 1);
		if (neighbour != null) {
			totalHeightNeighbours += neighbour.getHeight();
			numNeigbours++;
		}
		neighbour = get(x + 1, y - 1);
		if (neighbour != null) {
			totalHeightNeighbours += neighbour.getHeight();
			numNeigbours++;
		}
		neighbour = get(x - 1, y + 1);
		if (neighbour != null) {
			totalHeightNeighbours += neighbour.getHeight();
			numNeigbours++;
		}
		neighbour = get(x + 1, y + 1);
		if (neighbour != null) {
			totalHeightNeighbours += neighbour.getHeight();
			numNeigbours++;
		}

		float average = totalHeightNeighbours / numNeigbours;
		set(x, y, average);
	}

	public HeightGameMap toHeightMap() {
		HeightGameMap result = new HeightGameMap(numTilesWide, numTilesHigh);

		for (int x = 0; x < numTilesWide; x++) {
			for (int y = 0; y < numTilesHigh; y++) {
				result.setHeight(x, y, average(x, y));
			}
		}
		result.normalise();

		return result;
	}

	private float average(int x, int y) {
		return (get(x, y).getHeight() +
				get(x + 1, y).getHeight() +
				get(x, y + 1).getHeight() +
				get(x + 1, y + 1).getHeight()) / 4f;
	}

	public VertexGameMap crop(int offsetX, int offsetY, int targetWidth, int targetHeight) {
		VertexGameMap result = new VertexGameMap(targetWidth, targetHeight);

		for (int x = offsetX; x < offsetX + targetWidth + 1; x++) {
			for (int y = offsetY; y < offsetY + targetHeight + 1; y++) {
				result.set(x - offsetX, y - offsetY, get(x, y).getHeight());
			}
		}
		result.normalise();
		return result;
	}
}
