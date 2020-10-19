package technology.rocketjump.undermount.rooms;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.materials.model.PersistenceTestHarness;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class HaulingAllocationPersistenceTest extends PersistenceTestHarness {

	@Test
	public void test_persistence() throws InvalidSaveException {
		HaulingAllocation original = new HaulingAllocation();
		original.setSourcePositionType(HaulingAllocation.AllocationPositionType.FURNITURE);
		original.setSourcePosition(new GridPoint2(1, 2));
		original.setSourceContainerId(3L);
		original.setTargetPositionType(HaulingAllocation.AllocationPositionType.FLOOR);
		original.setTargetPosition(new GridPoint2(3, 4));
		original.setTargetId(5L);

		original.writeTo(stateHolder);
		JSONObject asJson = stateHolder.haulingAllocationsJson.getJSONObject(0);

		HaulingAllocation loaded = new HaulingAllocation();
		loaded.readFrom(asJson, stateHolder, dictionaries);

		assertThat(loaded).isEqualTo(original);
	}

}