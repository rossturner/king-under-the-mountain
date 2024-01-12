package technology.rocketjump.undermount.mapgen.model;

import com.badlogic.gdx.utils.Array;
import technology.rocketjump.undermount.mapgen.model.output.GameMap;
import technology.rocketjump.undermount.mapgen.model.output.GameMapTile;

public class HeightGameMap extends AbstractGameMap {

	private Array<Array<HeightMapTile>> xArray;

	public HeightGameMap(int width, int height) {
		super(width, height);
		xArray = new Array<>(width);
		for (int x = 0; x < width; x++) {
			Array<HeightMapTile> yArray = new Array<>(height);
			for (int y = 0; y < height; y++) {
				yArray.add(new HeightMapTile());
			}
			xArray.add(yArray);
		}
	}

	public float getHeight(int x, int y) {
		return xArray.get(x).get(y).getHeight();
	}

	public void setHeight(int x, int y, float height) {
		xArray.get(x).get(y).setHeight(height);
	}

	public void normalise() {
		float minHeight = Float.MAX_VALUE;
		float maxHeight = Float.MIN_VALUE;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				float heightAtPoint = getHeight(x, y);
				if (heightAtPoint < minHeight) {
					minHeight = heightAtPoint;
				}
				if (heightAtPoint > maxHeight) {
					maxHeight = heightAtPoint;
				}
			}
		}

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				float heightAtPoint = getHeight(x, y);
				float adjusted = (heightAtPoint - minHeight) / (maxHeight - minHeight);
				setHeight(x, y, adjusted);
			}
		}
	}

	/**
	 * This method attemps to find the correct height at which ~rationThreshold% of the map is above
	 *
	 * So when supplied 0.2, the returned height should be where 20% of the map is above return value
	 */
	public float heightForRatioAbove(float desiredRatio) {
		float testHeight = 0.5f;
		float binarySearchSize = 0.25f;
		float currentRatio = ratioOfHeightAbove(testHeight);
		while (binarySearchSize > EPSILON && !epsilonEquals(currentRatio, desiredRatio)) {
			if (currentRatio < desiredRatio) {
				testHeight -= binarySearchSize;
			} else {
				testHeight += binarySearchSize;
			}
			binarySearchSize = binarySearchSize / 2f;

			currentRatio = ratioOfHeightAbove(testHeight);
		}

		return testHeight;
	}

	public GameMap toGameMap(float mountainHeight) {
		GameMap gameMap = new GameMap(width, height);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				float tileHeight = getHeight(x, y);
				GameMapTile tile = gameMap.get(x, y);
				if (tileHeight > mountainHeight) {
					tile.setAsMountain();
				} else {
					tile.setAsOutside();
				}
				tile.setHeightMapValue(tileHeight - mountainHeight);
			}
		}

		gameMap.normaliseHeights();

		return gameMap;
	}

	private static final float EPSILON = 0.001f;
	public static boolean epsilonEquals(float a, float b) {
		return (Math.abs(a - b) < EPSILON);
	}

	public float ratioOfHeightAbove(float heightThreshold) {
		float total = 0f;
		float numberAboveThreshold = 0f;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				total++;
				if (getHeight(x, y) >= heightThreshold) {
					numberAboveThreshold++;
				}
			}
		}

		return numberAboveThreshold / total;
	}

	public HeightGameMap crop(int offsetX, int offsetY, int targetWidth, int targetHeight) {
		HeightGameMap result = new HeightGameMap(targetWidth, targetHeight);

		for (int x = offsetX; x < offsetX + targetWidth; x++) {
			for (int y = offsetY; y < offsetY + targetHeight; y++) {
				result.setHeight(x - offsetX, y - offsetY, getHeight(x, y));
			}
		}
		result.normalise();
		return result;
	}
}
