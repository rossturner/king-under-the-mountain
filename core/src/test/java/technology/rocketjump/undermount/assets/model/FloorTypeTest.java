package technology.rocketjump.undermount.assets.model;

import org.junit.Test;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.fest.assertions.Assertions.assertThat;

public class FloorTypeTest {

	@Test
	public void testTreeSetSortingAndEquality() {
		FloorOverlap def1 = buildFloorOverlap(3);
		FloorOverlap def2 = buildFloorOverlap(0);
		FloorOverlap def3 = buildFloorOverlap(1);
		FloorOverlap def4 = buildFloorOverlap(3);

		Set<FloorOverlap> treeSet = new TreeSet<>(new FloorType.FloorDefinitionComparator());
		treeSet.add(def1);
		treeSet.add(def2);
		treeSet.add(def3);
		treeSet.add(def4);

		assertThat(treeSet).hasSize(3);
		Iterator<FloorOverlap> iterator = treeSet.iterator();
		assertThat(iterator.next().getFloorType().getLayer()).isEqualTo(0);
		assertThat(iterator.next().getFloorType().getLayer()).isEqualTo(1);
		assertThat(iterator.next().getFloorType().getLayer()).isEqualTo(3);
	}

	private FloorOverlap buildFloorOverlap(int layer) {
		FloorType floorType = new FloorType("test", null, layer, GameMaterialType.OTHER, layer, 1,
				new OverlapType("none"), false, null, null);
		return new FloorOverlap(null, floorType, new GameMaterial("unused", -1L, GameMaterialType.OTHER), buildTestVertices());
	}

	private MapVertex[] buildTestVertices() {
		return new MapVertex[] {
				new MapVertex(0, 0),
				new MapVertex(0, 0),
				new MapVertex(0, 0),
				new MapVertex(0, 0)
		};
	}

}