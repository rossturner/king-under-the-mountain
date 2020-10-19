package technology.rocketjump.undermount.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.misc.Name;

import java.util.List;
import java.util.Map;

/**
 * This class determines which ScheduleCategories apply to each hour of the day
 */
public class Schedule {

	@Name
	private final String name;
	private final Map<Integer, List<ScheduleCategory>> hourlyCategories;

	@JsonCreator
	public Schedule(@JsonProperty("name") String name, @JsonProperty("hourlyCategories") Map<Integer, List<ScheduleCategory>> hourlyCategories) {
		this.name = name;
		this.hourlyCategories = hourlyCategories;
	}

	public String getName() {
		return name;
	}

	public Map<Integer, List<ScheduleCategory>> getHourlyCategories() {
		return hourlyCategories;
	}

	public List<ScheduleCategory> getCurrentApplicableCategories(GameClock clock) {
		int hourOfDay = clock.getHourOfDay();
		List<ScheduleCategory> categories = hourlyCategories.get(hourOfDay);
		if (categories == null) {
			// Might be working with a non-24 hour day or something else weirdly modded, just acting as midnight for now
			categories = hourlyCategories.get(0);
		}
		return categories;
	}
}
