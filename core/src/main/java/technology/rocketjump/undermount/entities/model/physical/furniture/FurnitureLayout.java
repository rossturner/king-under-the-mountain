package technology.rocketjump.undermount.entities.model.physical.furniture;

import com.badlogic.gdx.math.GridPoint2;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.misc.Name;

import java.util.ArrayList;
import java.util.List;

public class FurnitureLayout {

	@Name
	private String uniqueName;
	private String rotatesToName;
	@JsonIgnore
	private FurnitureLayout rotatesTo;

	private List<GridPoint2> extraTiles = new ArrayList<>();
	private List<FurnitureLayout.Workspace> workspaces = new ArrayList<>();

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public String getRotatesToName() {
		return rotatesToName;
	}

	public void setRotatesToName(String rotatesToName) {
		this.rotatesToName = rotatesToName;
	}

	public FurnitureLayout getRotatesTo() {
		return rotatesTo;
	}

	public void setRotatesTo(FurnitureLayout rotatesTo) {
		this.rotatesTo = rotatesTo;
	}

	public List<GridPoint2> getExtraTiles() {
		return extraTiles;
	}

	public void setExtraTiles(List<GridPoint2> extraTiles) {
		this.extraTiles = extraTiles;
	}

	public List<Workspace> getWorkspaces() {
		return workspaces;
	}

	public void setWorkspaces(List<Workspace> workspaces) {
		this.workspaces = workspaces;
	}

	public static class Workspace {

		private GridPoint2 location;
		private GridPoint2 accessedFrom;

		public GridPoint2 getLocation() {
			return location;
		}

		public void setLocation(GridPoint2 location) {
			this.location = location;
		}

		public GridPoint2 getAccessedFrom() {
			return accessedFrom;
		}

		public void setAccessedFrom(GridPoint2 accessedFrom) {
			this.accessedFrom = accessedFrom;
		}

		@Override
		public String toString() {
			return "Location: " + location + ", Accessed from " + accessedFrom;
		}
	}

	@Override
	public String toString() {
		return "FurnitureLayout{" +
				"uniqueName='" + uniqueName + '\'' +
				'}';
	}
}
