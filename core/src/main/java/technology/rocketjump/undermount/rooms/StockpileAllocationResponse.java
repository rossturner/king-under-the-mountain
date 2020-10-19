package technology.rocketjump.undermount.rooms;

import com.badlogic.gdx.math.GridPoint2;

public class StockpileAllocationResponse {

	public final GridPoint2 position;
	public final int quantity;

	public StockpileAllocationResponse(GridPoint2 position, int quantity) {
		this.position = position;
		this.quantity = quantity;
	}
}
