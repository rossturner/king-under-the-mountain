package technology.rocketjump.undermount.entities.ai.goap;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ScheduleDictionaryTest {
	@Test
	public void getDefaultSettlerSchedule() throws Exception {
		ScheduleDictionary scheduleDictionary = new ScheduleDictionary();

		Schedule settlerSchedule = scheduleDictionary.settlerSchedule;

		assertThat(settlerSchedule).isNotNull();
	}

}