package technology.rocketjump.undermount.jobs.model;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.PersistenceTestHarness;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JobTest extends PersistenceTestHarness {

	@Test
	public void testPersistence() throws InvalidSaveException {
		JobType jobType = new JobType();
		jobType.setName("Test");
		when(mockJobTypeDictionary.getByName("Test")).thenReturn(jobType);
		when(mockMaterialDictionary.getByName(GameMaterial.NULL_MATERIAL.getMaterialName())).thenReturn(GameMaterial.NULL_MATERIAL);

		Job original = new Job(jobType);
		original.setWorkDoneSoFar(1);
		original.setAssignedToEntityId(3L);
		original.setTargetId(4L);
		original.setJobState(JobState.ASSIGNED);
		original.setRequiredItemMaterial(GameMaterial.NULL_MATERIAL);
		original.setJobLocation(new GridPoint2(1, 2));

		original.writeTo(stateHolder);
		JSONObject asJson = stateHolder.jobsJson.getJSONObject(0);

		Job loaded = new Job();
		loaded.readFrom(asJson, stateHolder, dictionaries);

		assertThat(loaded).isEqualTo(original);
	}
}