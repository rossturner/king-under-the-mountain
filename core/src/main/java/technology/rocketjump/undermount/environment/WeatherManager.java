package technology.rocketjump.undermount.environment;


import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.environment.model.WeatherType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WeatherManager implements Updatable {

	private GameContext gameContext;

	private static final float TOTAL_COLOR_CHANGE_TIME = 10f;

	@Inject
	public WeatherManager() {

	}

	@Override
	public void update(float deltaTime) {
		if (gameContext != null) {
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
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
