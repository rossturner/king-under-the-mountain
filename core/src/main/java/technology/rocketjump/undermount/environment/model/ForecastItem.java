package technology.rocketjump.undermount.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ForecastItem {

	private String weatherTypeName;
	@JsonIgnore
	private WeatherType weatherType;
	private double minDurationHours;
	private double maxDurationHours;
	private float chance;

	public String getWeatherTypeName() {
		return weatherTypeName;
	}

	public void setWeatherTypeName(String weatherTypeName) {
		this.weatherTypeName = weatherTypeName;
	}

	public double getMinDurationHours() {
		return minDurationHours;
	}

	public void setMinDurationHours(double minDurationHours) {
		this.minDurationHours = minDurationHours;
	}

	public double getMaxDurationHours() {
		return maxDurationHours;
	}

	public void setMaxDurationHours(double maxDurationHours) {
		this.maxDurationHours = maxDurationHours;
	}

	public WeatherType getWeatherType() {
		return weatherType;
	}

	public void setWeatherType(WeatherType weatherType) {
		this.weatherType = weatherType;
	}

	public float getChance() {
		return chance;
	}

	public void setChance(float chance) {
		this.chance = chance;
	}
}
