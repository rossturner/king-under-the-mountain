package technology.rocketjump.undermount.entities.ai.goap;

import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ScheduleDictionary {

	private static final String DEFAULT_SETTLER_SCHEDULE_NAME = "Default Settler Schedule";
	private Map<String, Schedule> byName = new HashMap<>();
	public final Schedule settlerSchedule;

	@Inject
	public ScheduleDictionary() throws IOException {

		FileHandle schedulesJsonFile = new FileHandle("assets/ai/schedules.json");
		ObjectMapper objectMapper = new ObjectMapper();
		List<Schedule> schedules = objectMapper.readValue(schedulesJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, Schedule.class));

		for (Schedule schedule : schedules) {
			byName.put(schedule.getName(), schedule);
		}

		settlerSchedule = byName.get(DEFAULT_SETTLER_SCHEDULE_NAME);
		if (settlerSchedule == null) {
			throw new RuntimeException("Could not find schedule with name: " + DEFAULT_SETTLER_SCHEDULE_NAME);
		}
	}

	public Schedule getByName(String name) {
		return byName.get(name);
	}

}
