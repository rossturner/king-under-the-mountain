package technology.rocketjump.undermount.assets.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomEdgeType {

	private final String roomEdgeTypeName;
	private long roomEdgeTypeId;

	@JsonCreator
	public RoomEdgeType(@JsonProperty("roomEdgeTypeName") String roomEdgeTypeName,
						@JsonProperty("roomEdgeTypeId") long roomEdgeTypeId) {
		this.roomEdgeTypeName = roomEdgeTypeName;
		this.roomEdgeTypeId = roomEdgeTypeId;
	}

	public String getRoomEdgeTypeName() {
		return roomEdgeTypeName;
	}

	public long getRoomEdgeTypeId() {
		return roomEdgeTypeId;
	}


	public void setRoomEdgeTypeId(long id) {
		this.roomEdgeTypeId = id;
	}

	@Override
	public String toString() {
		return roomEdgeTypeName + ", roomEdgeTypeId=" + roomEdgeTypeId;
	}
}
