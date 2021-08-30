package technology.rocketjump.undermount.entities.model.physical.furniture;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.rendering.utils.HexColors;

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
	private List<FurnitureLayout.SpecialTile> specialTiles = new ArrayList<>();

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

	public List<SpecialTile> getSpecialTiles() {
		return specialTiles;
	}

	public void setSpecialTiles(List<SpecialTile> specialTiles) {
		this.specialTiles = specialTiles;
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

	public static class SpecialTile {

		private GridPoint2 location;
		private SpecialTileRequirment requirement;

		public enum SpecialTileRequirment {

			IS_RIVER(mapTile -> mapTile.getFloor().isRiverTile(), HexColors.get("#26e1ed"));

			public final TileCheck tileCheck;
			public final Color color;

			private SpecialTileRequirment(TileCheck tileCheck, Color color) {
				this.tileCheck = tileCheck;
				this.color = color;
			}

			public interface TileCheck {
				boolean isValid(MapTile mapTile);
			}

		}

		public GridPoint2 getLocation() {
			return location;
		}

		public void setLocation(GridPoint2 location) {
			this.location = location;
		}

		public SpecialTileRequirment getRequirement() {
			return requirement;
		}

		public void setRequirement(SpecialTileRequirment requirement) {
			this.requirement = requirement;
		}

		@Override
		public String toString() {
			return "Location: " + location + ", Requirement " + requirement.name();
		}
	}

	@Override
	public String toString() {
		return "FurnitureLayout{" +
				"uniqueName='" + uniqueName + '\'' +
				'}';
	}
}
