package technology.rocketjump.undermount.mapgen.generators;

import technology.rocketjump.undermount.mapgen.model.HeightmapVertex;
import technology.rocketjump.undermount.mapgen.model.VertexGameMap;

import java.util.Random;

public class MidpointDisplacement {

	/**
	 * This method takes input and returns a vertex map of twice the dimensions,
	 * with points inbetween set to the average of their neighbours
	 */
	public VertexGameMap doubleSize(VertexGameMap input) {
		VertexGameMap output = new VertexGameMap(input.getNumTilesWide() * 2, input.getNumTilesHigh() * 2);

		for (int inputXCursor = 0; inputXCursor <= input.getNumTilesWide(); inputXCursor++) {
			for (int inputYCursor = 0; inputYCursor <= input.getNumTilesHigh(); inputYCursor++) {
				output.set(inputXCursor * 2, inputYCursor * 2, input.get(inputXCursor, inputYCursor).getHeight());
			}
		}

		// Set middle vertex of points that have just been set
		for (int inputXCursor = 0; inputXCursor < input.getNumTilesWide(); inputXCursor++) {
			for (int inputYCursor = 0; inputYCursor < input.getNumTilesHigh(); inputYCursor++) {
				output.setAverageOfSquare((inputXCursor * 2) + 1, (inputYCursor * 2) + 1);
			}
		}

		// Set inner points of output based on average of input
		for (int inputXCursor = 0; inputXCursor < input.getNumTilesWide(); inputXCursor++) {
			for (int inputYCursor = 0; inputYCursor < input.getNumTilesHigh(); inputYCursor++) {
				output.setAverageOfDiamond((inputXCursor * 2) + 1, inputYCursor * 2);
				output.setAverageOfDiamond(inputXCursor * 2, (inputYCursor * 2) + 1);
			}
		}
		return output;
	}

	/**
	 * This method applies diamond square to input which is assumed to already have "in-between" vertices set to the average of their neighbours
	 */
	public void applyDiamondSquareToPredoubled(VertexGameMap output, float maxVariance, Random random) {
		// Set middle vertex of points that have just been set
		for (int inputXCursor = 0; inputXCursor < output.getNumTilesWide() / 2; inputXCursor++) {
			for (int inputYCursor = 0; inputYCursor < output.getNumTilesHigh() / 2; inputYCursor++) {
				HeightmapVertex vertex = output.get((inputXCursor * 2) + 1, (inputYCursor * 2) + 1);
				vertex.setHeight(vary(vertex.getHeight(), maxVariance, random));
			}
		}

		// Set inner points of output based on average of input
		for (int inputXCursor = 0; inputXCursor < output.getNumTilesWide() / 2; inputXCursor++) {
			for (int inputYCursor = 0; inputYCursor < output.getNumTilesHigh() / 2; inputYCursor++) {
				// Need to reset average of diamond due to adjusted square midpoint
				output.setAverageOfDiamond((inputXCursor * 2) + 1, inputYCursor * 2);
				HeightmapVertex vertex = output.get((inputXCursor * 2) + 1, inputYCursor * 2);
				if (vertex != null) {
					vertex.setHeight(vary(vertex.getHeight(), maxVariance, random));
				}


				output.setAverageOfDiamond(inputXCursor * 2, (inputYCursor * 2) + 1);
				vertex = output.get(inputXCursor * 2, (inputYCursor * 2) + 1);
				if (vertex != null) {
					vertex.setHeight(vary(vertex.getHeight(), maxVariance, random));
				}
			}
		}

	}

	public static float vary(float input, float maxVariance, Random random) {
		float varianceAmount = (random.nextFloat() * (2 * maxVariance)) - maxVariance;
		return input + varianceAmount;
	}
}
