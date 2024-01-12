package technology.rocketjump.undermount.mapgen.model;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;

/**
 * Represents a set of 2D grid points that are either true or false
 */
public class BinaryGrid {

	private Array<Array<Boolean>> xArray;

	public BinaryGrid(Array<Array<Boolean>> xArray) {
		this.xArray = xArray;
	}

	public int getWidth() {
		return xArray.size;
	}

	public int getHeight() {
		return xArray.get(0).size;
	}

	public int getNumTrue() {
		int numTrue = 0;
		for (Array<Boolean> yArray : xArray) {
			for (Boolean aBoolean : yArray) {
				if (aBoolean) {
					numTrue++;
				}
			}
		}
		return numTrue;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (int x = 0; x < xArray.size; x++) {
			Array<Boolean> yArray = xArray.get(x);
			for (int y = 0; y < yArray.size; y++) {
				if (yArray.get(y)) {
					builder.append("X");
				} else {
					builder.append(".");
				}
			}
			builder.append("\n");
		}

		return builder.toString();
	}

	/**
	 * This function adds an increasing number of blanks before each line to skew to a total amount
 	 */
	public void skew(float amountToSkewBy) {
		int largestArraySize = 0;
		for (int x = 0; x < xArray.size; x++) {
			Array<Boolean> yArray = xArray.get(x);
			int tilesToSkewBy = Math.round(amountToSkewBy * ((float)x / (float)xArray.size));
			while (tilesToSkewBy > 0) {
				yArray.insert(0, false);
				tilesToSkewBy--;
			}
			if (yArray.size > largestArraySize) {
				largestArraySize = yArray.size;
			}
		}

		// Pad out so all arrays are same length
		for (int x = 0; x < xArray.size; x++) {
			Array<Boolean> yArray = xArray.get(x);
			while (yArray.size < largestArraySize) {
				yArray.add(false);
			}
		}
	}

	public void transpose() {
		Array<Array<Boolean>> rotated = new Array<>();

		int rotatedSize = xArray.get(0).size;

		for (int cursor = 0; cursor < rotatedSize; cursor++) {
			rotated.add(new Array<Boolean>());
		}

		for (int x = 0; x < xArray.size; x++) {
			Array<Boolean> currentYArray = xArray.get(x);
			for (int y = 0; y < currentYArray.size; y++) {
				rotated.get(y).add(currentYArray.get(y));
			}
		}

		this.xArray = rotated;
	}

	public void reverseEachRow() {

		for (int x = 0; x < xArray.size; x++) {
			Array<Boolean> currentYArray = xArray.get(x);
			Array<Boolean> reversedYArray = new Array<>(currentYArray.size);
			for (int y = currentYArray.size - 1; y >= 0; y--) {
				reversedYArray.add(currentYArray.get(y));
			}
			xArray.set(x, reversedYArray);
		}
	}

	public void rotate90() {
		transpose();
		reverseEachRow();
	}

	public boolean get(int x, int y) {
		Array<Boolean> yArray = xArray.get(x);
		if (yArray == null) {
			return false;
		} else {
			return yArray.get(y);
		}
	}
}
