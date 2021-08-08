package technology.rocketjump.undermount.environment.model;

import technology.rocketjump.undermount.misc.Name;

import java.util.ArrayList;
import java.util.List;

public class DailyWeatherType {

	@Name
	private String name;

	private Season applicableSeason;

	private float chance;

	private List<ForecastItem> forecast = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ForecastItem> getForecast() {
		return forecast;
	}

	public void setForecast(List<ForecastItem> forecast) {
		this.forecast = forecast;
	}

	public Season getApplicableSeason() {
		return applicableSeason;
	}

	public void setApplicableSeason(Season applicableSeason) {
		this.applicableSeason = applicableSeason;
	}

	public float getChance() {
		return chance;
	}

	public void setChance(float chance) {
		this.chance = chance;
	}
}
