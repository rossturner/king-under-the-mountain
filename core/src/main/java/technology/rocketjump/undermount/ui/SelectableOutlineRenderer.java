package technology.rocketjump.undermount.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;

import java.util.HashSet;
import java.util.Set;

import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class SelectableOutlineRenderer {

	public void render(Selectable selectable, ShapeRenderer shapeRenderer) {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.WHITE);

		switch (selectable.type) {
			case ENTITY:
				Entity selectedEntity = selectable.getEntity();

				Vector2 worldPosition = selectedEntity.getLocationComponent().getWorldPosition();
				if (worldPosition == null) {
					break;
				}
				switch (selectedEntity.getType()) {
					case ITEM:
					case PLANT:
					case ANIMAL:
					case HUMANOID:
						shapeRenderer.circle(worldPosition.x, worldPosition.y,
								selectedEntity.getLocationComponent().getRadius() + 0.3f, 100);
						break;
					case FURNITURE:
						Set<GridPoint2> entityLocations = new HashSet<>();
						GridPoint2 entityLocation = toGridPoint(worldPosition);
						entityLocations.add(entityLocation);
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) selectedEntity.getPhysicalEntityComponent().getAttributes();
						for (GridPoint2 extraTileOffset : attributes.getCurrentLayout().getExtraTiles()) {
							entityLocations.add(entityLocation.cpy().add(extraTileOffset));
						}
						renderTileOutline(entityLocations, shapeRenderer);
						break;
				}
				break;
			case ROOM:
				renderTileOutline(selectable.getRoom().getRoomTiles().keySet(), shapeRenderer);
				break;
			case BRIDGE:
				renderTileOutline(selectable.getBridge().getLocations(), shapeRenderer);
				break;
			case TILE:
				renderTileOutline(selectable.getTile().getTilePosition(), shapeRenderer);
				break;
			case DOORWAY:
				renderTileOutline(selectable.getDoorway().getTileLocation(), shapeRenderer);
				break;
			case CONSTRUCTION:
				renderTileOutline(selectable.getConstruction().getTileLocations(), shapeRenderer);
				break;
			default:
				Logger.error("Not yet implemented: " + this.getClass().getSimpleName() + ".render() for " + selectable.type);
		}

		shapeRenderer.end();
	}

	private void renderTileOutline(GridPoint2 tileLocation, ShapeRenderer shapeRenderer) {
		shapeRenderer.rect(tileLocation.x, tileLocation.y, 1,1);
	}

	private void renderTileOutline(Set<GridPoint2> tileLocations, ShapeRenderer shapeRenderer) {
		for (GridPoint2 tileLocation : tileLocations) {

			// Draw top
			if (!tileLocations.contains(tileLocation.cpy().add(0, 1))) {
				shapeRenderer.line(tileLocation.x, tileLocation.y + 1, tileLocation.x + 1, tileLocation.y + 1);
			}
			// draw right
			if (!tileLocations.contains(tileLocation.cpy().add(1, 0))) {
				shapeRenderer.line(tileLocation.x + 1, tileLocation.y, tileLocation.x + 1, tileLocation.y + 1);
			}
			// draw left
			if (!tileLocations.contains(tileLocation.cpy().add(-1, 0))) {
				shapeRenderer.line(tileLocation.x, tileLocation.y, tileLocation.x, tileLocation.y + 1);
			}
			// draw bottom
			if (!tileLocations.contains(tileLocation.cpy().add(0, -1))) {
				shapeRenderer.line(tileLocation.x, tileLocation.y, tileLocation.x + 1, tileLocation.y);
			}

		}
	}

}
