package technology.rocketjump.undermount.mapgen.generators;

import com.badlogic.gdx.utils.Array;
import technology.rocketjump.undermount.mapgen.model.BinaryGrid;

import java.util.Random;

/**
 * This class generates a set of points within a rectangle to represent a vein of ore within rock
 *
 * First it calculates an amount of thickness across the specified length, using the max and variance
 *
 * Then this is "rotated" by moving one end of points and rotating the final result in 90 degree angles
 */
public class OreVeinGenerator {

	public BinaryGrid generate(int length, float inputThickness, float variance, Random random) {

		Array<Integer> thicknesses = new Array<>(length);
		int actualMaxThickness = 0;
		for (int cursor = 0; cursor < length; cursor++) {
			// This gives 0 near center and 1 near extreme
			float distanceToCenter = Math.abs(cursor - ((float)length / 2f));
			float normalisedDistanceToCenter = distanceToCenter / ((float)length / 2f);
			float normalisedNearnessToCenter = 1 - normalisedDistanceToCenter;

			float thickness = (normalisedNearnessToCenter * inputThickness) + ( ((random.nextFloat() * 2) - 1) * variance );
			int actualThickness = Math.max(1, Math.round(Math.abs(thickness)));
			if (actualThickness > actualMaxThickness) {
				actualMaxThickness = actualThickness;
			}
			thicknesses.add(actualThickness);
		}

		Array<Array<Boolean>> xArray = new Array<>(length);

		for (int columnCursor = 0; columnCursor < length; columnCursor++) {
			Array<Boolean> yArray = new Array<>(actualMaxThickness);
			Integer thicknessToProduce = thicknesses.get(columnCursor);
			int blanksToAdd = actualMaxThickness - thicknessToProduce;
			int blanksToAddBefore = blanksToAdd / 2;
			if (blanksToAdd % 2 == 1) {
				// There are even num of blanks to add, so add an extra one before or after
				if (random.nextBoolean()) {
					blanksToAddBefore++;
				}
			}

			for (int cellCursor = 0; cellCursor < actualMaxThickness; cellCursor++) {
				if (cellCursor < blanksToAddBefore) {
					yArray.add(false);
				} else if (cellCursor < blanksToAddBefore + thicknessToProduce) {
					yArray.add(true);
				} else {
					yArray.add(false);
				}
			}

			xArray.add(yArray);

		}

		BinaryGrid grid = new BinaryGrid(xArray);

		float amountToSkewBy = random.nextFloat() * length;
		grid.skew(amountToSkewBy);

		int numTimeToRotate = random.nextInt(3);
		while (numTimeToRotate > 0) {
			grid.rotate90();
			numTimeToRotate--;
		}

		return grid;
	}
}
