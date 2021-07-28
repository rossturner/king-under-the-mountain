package technology.rocketjump.undermount.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import technology.rocketjump.undermount.environment.model.DailyWeatherType;
import technology.rocketjump.undermount.environment.model.ForecastItem;
import technology.rocketjump.undermount.environment.model.WeatherType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Singleton
public class DailyWeatherTypeDictionary {

	private final Map<String, DailyWeatherType> byName = new HashMap<>();
	private final WeatherTypeDictionary weatherTypeDictionary;

	@Inject
	public DailyWeatherTypeDictionary(WeatherTypeDictionary weatherTypeDictionary) throws IOException {
		this.weatherTypeDictionary = weatherTypeDictionary;
		ObjectMapper objectMapper = new ObjectMapper();
		FileHandle weatherTypesFile = Gdx.files.internal("assets/settings/dailyWeather.json");
		List<DailyWeatherType> weatherTypes = objectMapper.readValue(weatherTypesFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, DailyWeatherType.class));

		for (DailyWeatherType weatherType : weatherTypes) {
			initalise(weatherType);
			byName.put(weatherType.getName(), weatherType);
		}

	}

	public DailyWeatherType getByName(String weatherTypeName) {
		return byName.get(weatherTypeName);
	}

	public Collection<DailyWeatherType> getAll() {
		return byName.values();
	}

	private void initalise(DailyWeatherType dailyWeatherType) {
		if (dailyWeatherType.getForecast().isEmpty()) {
			throw new RuntimeException("Forecast for " + dailyWeatherType.getName() + " can not be empty");
		}

		for (ForecastItem forecastItem : dailyWeatherType.getForecast()) {
			WeatherType weatherType = weatherTypeDictionary.getByName(forecastItem.getWeatherTypeName());
			if (weatherType == null) {
				throw new RuntimeException("Could not find weather type with name " + forecastItem.getWeatherTypeName() +
						" for " + DailyWeatherType.class.getSimpleName() + ": " + dailyWeatherType.getName());
			} else {
				forecastItem.setWeatherType(weatherType);
			}
		}

	}
}
