package technology.rocketjump.undermount.mapping.minimap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileExploration;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.Room;

import java.util.ArrayList;

import static com.badlogic.gdx.graphics.Color.rgba8888;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

public class MinimapPixmapGenerator {

	private static final int UNEXPLORED_COLOR = rgba8888(HexColors.get("#302b32"));
	private static final int RIVER_COLOR = rgba8888(HexColors.get("#67c6d0"));
	private static final int DEFAULT_GROUND_COLOR = rgba8888(HexColors.get("#637c4d"));
	private static final int TREE_COLOR = rgba8888(Color.BROWN);

	public static Pixmap generateFrom(TiledMap areaMap) {
		Pixmap pixmap = new Pixmap(areaMap.getWidth(), areaMap.getHeight(), Pixmap.Format.RGBA8888);
		for (int y = 0; y < areaMap.getHeight(); y++) {
			for (int x = 0; x < areaMap.getWidth(); x++) {
				int color = pickColor(areaMap.getTile(x, y));
				pixmap.drawPixel(x, areaMap.getHeight() - 1 - y, color);
			}
		}
		return pixmap;
	}

	private static int pickColor(MapTile tile) {
		if (tile.getExploration().equals(TileExploration.UNEXPLORED)) {
			return UNEXPLORED_COLOR;

		} else if (tile.hasWall()) {
			if (tile.getWall().hasOre()) {
				return rgba8888(tile.getWall().getOreMaterial().getColor());
			} else {
				return rgba8888(tile.getWall().getMaterial().getColor());
			}
		} else if (tile.hasRoom()) {
			Room room = tile.getRoomTile().getRoom();
			if (room != null) {
				return rgba8888(room.getRoomType().getColor());
			} else {
				Logger.error("Room is null in " + MinimapPixmapGenerator.class.getSimpleName());
				return DEFAULT_GROUND_COLOR;
			}
		} else if (tile.hasDoorway()) {
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) tile.getDoorway().getDoorEntity().getPhysicalEntityComponent().getAttributes();
			return rgba8888(attributes.getMaterials().get(attributes.getPrimaryMaterialType()).getColor());

		} else if (tile.getFloor().hasBridge()) {
			return rgba8888(tile.getFloor().getBridge().getMaterial().getColor());
		} else if (tile.getFloor().isRiverTile()) {
			return RIVER_COLOR;
		} else {
			for (Entity entity : new ArrayList<>(tile.getEntities()) ) {
				if (entity.getType().equals(EntityType.PLANT)) {
					PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.TREE)) {
						return TREE_COLOR;
					}
				}
			}

			if (tile.getFloor().getMaterial().getColor() == null || NULL_MATERIAL.equals(tile.getFloor().getMaterial())) {
				return DEFAULT_GROUND_COLOR;
			} else {
				return rgba8888(tile.getFloor().getMaterial().getColor());
			}

		}
	}

}
