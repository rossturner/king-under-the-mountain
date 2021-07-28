package technology.rocketjump.undermount.mapping.model;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.environment.model.WeatherType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.HexColors;

/**
 * This class is to store data such as the ambient temperature, weather, etc.
 */
public class MapEnvironment implements ChildPersistable {

	private WeatherType currentWeather;

	private Color sunlightColor = Color.WHITE.cpy();
	private Color weatherColor = Color.WHITE.cpy();
	private float sunlightAmount = 1;

	public WeatherType getCurrentWeather() {
		return currentWeather;
	}

	public void setCurrentWeather(WeatherType currentWeather) {
		this.currentWeather = currentWeather;
	}

	public Color getSunlightColor() {
		return sunlightColor;
	}

	private void setSunlightColor(Color sunlightColor) {
		this.sunlightColor = sunlightColor;
		this.sunlightAmount = (sunlightColor.r + sunlightColor.g + sunlightColor.b) / 3f;
	}

	public float getSunlightAmount() {
		return sunlightAmount;
	}

	public Color getWeatherColor() {
		return weatherColor;
	}

	public void setWeatherColor(Color weatherColor) {
		this.weatherColor = weatherColor;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("weather", currentWeather.getName());
		asJson.put("sunlight", HexColors.toHexString(sunlightColor));
		asJson.put("weatherColor", HexColors.toHexString(weatherColor));
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.currentWeather = relatedStores.weatherTypeDictionary.getByName(asJson.getString("weather"));
		if (this.currentWeather == null) {
			throw new InvalidSaveException("Could not find weather type with name " + asJson.getString("weather"));
		}
		setSunlightColor(HexColors.get(asJson.getString("sunlight")));
		setWeatherColor(HexColors.get(asJson.getString("weatherColor")));
	}
}
