package technology.rocketjump.undermount.environment;


import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.environment.model.ForecastItem;
import technology.rocketjump.undermount.environment.model.WeatherType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.rendering.ScreenWriter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class WeatherManager implements Updatable {

	private GameContext gameContext;

	private final DailyWeatherTypeDictionary dailyWeatherTypeDictionary;
	private final WeatherEffectUpdater weatherEffectUpdater;
	private final ScreenWriter screenWriter;

	private static final float TOTAL_COLOR_CHANGE_TIME = 10f;
	private double lastUpdateGameTime;

	@Inject
	public WeatherManager(DailyWeatherTypeDictionary dailyWeatherTypeDictionary, WeatherEffectUpdater weatherEffectUpdater, ScreenWriter screenWriter) {
		this.dailyWeatherTypeDictionary = dailyWeatherTypeDictionary;
		this.weatherEffectUpdater = weatherEffectUpdater;
		this.screenWriter = screenWriter;
	}

	@Override
	public void update(float deltaTime) {
		if (gameContext != null) {
			double currentGameTime = gameContext.getGameClock().getCurrentGameTime();
			double elapsedGameTime = currentGameTime - lastUpdateGameTime;
			this.lastUpdateGameTime = currentGameTime;

			gameContext.getMapEnvironment().setWeatherTimeRemaining(gameContext.getMapEnvironment().getWeatherTimeRemaining() - elapsedGameTime);

			screenWriter.printLine(gameContext.getMapEnvironment().getCurrentWeather().getName() + ", remaining: " + gameContext.getMapEnvironment().getWeatherTimeRemaining());

			if (gameContext.getMapEnvironment().getWeatherTimeRemaining() < 0) {
				triggerNextWeather();
			}

			updateWeatherColor(deltaTime);
		}
	}

	public void triggerNextWeather() {
		WeatherType currentWeather = gameContext.getMapEnvironment().getCurrentWeather();
		List<ForecastItem> forecast = gameContext.getMapEnvironment().getDailyWeather().getForecast();

		ForecastItem selectedForecast = null;
		if (forecast.size() == 1) {
			selectedForecast = forecast.get(0);
		} else {
			while (selectedForecast == null || selectedForecast.getWeatherType().equals(currentWeather)) {
				selectedForecast = forecast.get(gameContext.getRandom().nextInt(forecast.size()));
			}
		}

		double nextWeatherDuration = selectedForecast.getMinDurationHours();
		nextWeatherDuration += gameContext.getRandom().nextDouble() * Math.abs(selectedForecast.getMaxDurationHours() - selectedForecast.getMinDurationHours());

		gameContext.getMapEnvironment().setCurrentWeather(selectedForecast.getWeatherType());
		gameContext.getMapEnvironment().setWeatherTimeRemaining(nextWeatherDuration);
		weatherEffectUpdater.weatherChanged();
	}

	private void updateWeatherColor(float deltaTime) {
		WeatherType currentWeather = gameContext.getMapEnvironment().getCurrentWeather();
		Color targetWeatherColor = currentWeather.getMaxSunlightColor();
		Color currentWeatherColor = gameContext.getMapEnvironment().getWeatherColor();

		if (!currentWeatherColor.equals(targetWeatherColor)) {

			float changeAmount = deltaTime / TOTAL_COLOR_CHANGE_TIME;

			float channelDiff = targetWeatherColor.r - currentWeatherColor.r;
			if (Math.abs(channelDiff) <= changeAmount) {
				currentWeatherColor.r = targetWeatherColor.r;
			} else if (channelDiff < 0) {
				currentWeatherColor.r -= changeAmount;
			} else {
				currentWeatherColor.r += changeAmount;
			}

			channelDiff = targetWeatherColor.g - currentWeatherColor.g;
			if (Math.abs(channelDiff) <= changeAmount) {
				currentWeatherColor.g = targetWeatherColor.g;
			} else if (channelDiff < 0) {
				currentWeatherColor.g -= changeAmount;
			} else {
				currentWeatherColor.g += changeAmount;
			}

			channelDiff = targetWeatherColor.b - currentWeatherColor.b;
			if (Math.abs(channelDiff) <= changeAmount) {
				currentWeatherColor.b = targetWeatherColor.b;
			} else if (channelDiff < 0) {
				currentWeatherColor.b -= changeAmount;
			} else {
				currentWeatherColor.b += changeAmount;
			}

		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	@Override
	public void clearContextRelatedState() {
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
