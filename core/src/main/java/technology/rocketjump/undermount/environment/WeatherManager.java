package technology.rocketjump.undermount.environment;


import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.assets.FloorTypeDictionary;
import technology.rocketjump.undermount.assets.model.FloorType;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.environment.model.ForecastItem;
import technology.rocketjump.undermount.environment.model.WeatherType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileExploration;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ParticleRequestMessage;
import technology.rocketjump.undermount.messaging.types.ReplaceFloorMessage;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;
import technology.rocketjump.undermount.rendering.ScreenWriter;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class WeatherManager implements Updatable {

	private final GameMaterial snowMaterialType;
	private final FloorType snowFloorType;
	private GameContext gameContext;

	private final DailyWeatherTypeDictionary dailyWeatherTypeDictionary;
	private final WeatherEffectUpdater weatherEffectUpdater;
	private final ScreenWriter screenWriter;
	private final MessageDispatcher messageDispatcher;

	private static final float TOTAL_COLOR_CHANGE_TIME = 10f;
	private double lastUpdateGameTime;
	private Double timeToNextLightningStrike;
	private ParticleEffectType lightningEffectType;
	private SoundAsset thunderCrackSoundAsset;

	@Inject
	public WeatherManager(DailyWeatherTypeDictionary dailyWeatherTypeDictionary, WeatherEffectUpdater weatherEffectUpdater,
						  ScreenWriter screenWriter, FloorTypeDictionary floorTypeDictionary,
						  GameMaterialDictionary gameMaterialDictionary, MessageDispatcher messageDispatcher,
						  ParticleEffectTypeDictionary particleEffectTypeDictionary, SoundAssetDictionary soundAssetDictionary) {
		this.dailyWeatherTypeDictionary = dailyWeatherTypeDictionary;
		this.weatherEffectUpdater = weatherEffectUpdater;
		this.screenWriter = screenWriter;

		snowFloorType = floorTypeDictionary.getByFloorTypeName("fallen_snow");
		snowMaterialType = gameMaterialDictionary.getByName("Snowfall");
		lightningEffectType = particleEffectTypeDictionary.getByName("Lightning strike");
		thunderCrackSoundAsset = soundAssetDictionary.getByName("Thundercrack");
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public void update(float deltaTime) {
		if (gameContext != null) {
			double currentGameTime = gameContext.getGameClock().getCurrentGameTime();
			double elapsedGameTime = currentGameTime - lastUpdateGameTime;
			this.lastUpdateGameTime = currentGameTime;

			gameContext.getMapEnvironment().setWeatherTimeRemaining(gameContext.getMapEnvironment().getWeatherTimeRemaining() - elapsedGameTime);

			if (gameContext.getMapEnvironment().getCurrentWeather().getAccumulatesSnowPerHour() != null) {
				updateSnowfall(elapsedGameTime);
			}
			if (gameContext.getMapEnvironment().getCurrentWeather().getLightningStrikesPerHour() != null) {
				if (timeToNextLightningStrike == null) {
					timeToNextLightningStrike = getTimeToNextLightningStrike();
				}
				timeToNextLightningStrike -= elapsedGameTime;
				if (timeToNextLightningStrike < 0) {
					triggerLightningStrike();
					timeToNextLightningStrike = null;
				}
			}

			if (GlobalSettings.DEV_MODE) {
				screenWriter.printLine(gameContext.getMapEnvironment().getCurrentWeather().getName() + ", remaining: " + gameContext.getMapEnvironment().getWeatherTimeRemaining());
				screenWriter.printLine("Time to next strike: " + timeToNextLightningStrike);
			}

			if (gameContext.getMapEnvironment().getWeatherTimeRemaining() < 0) {
				triggerNextWeather();
			}

			updateWeatherColor(deltaTime);
		}
	}

	private double getTimeToNextLightningStrike() {
		return gameContext.getRandom().nextDouble() * ((1.0 / gameContext.getMapEnvironment().getCurrentWeather().getLightningStrikesPerHour()) * 2.0);
	}

	private void updateSnowfall(double elapsedGameTime) {
		double extraSnow = gameContext.getMapEnvironment().getCurrentWeather().getAccumulatesSnowPerHour() * elapsedGameTime;
		double currentSnow = gameContext.getMapEnvironment().getFallenSnow();
		double newSnow = currentSnow + extraSnow;
		newSnow = Math.max(0.0, Math.min(newSnow, 1.0));

		int currentSnowPercentile = toSnowPercentile(currentSnow);
		int newSnowPercentile = toSnowPercentile(newSnow);

		if (newSnowPercentile != currentSnowPercentile) {
			if (newSnowPercentile > currentSnowPercentile) {
				// Increasing snowfall
				for (int percentileToAdd = currentSnowPercentile; percentileToAdd < newSnowPercentile || percentileToAdd == 100; percentileToAdd++) {
					addSnowToGround(percentileToAdd);
				}
			} else {
				// Decreasing snowfall
				for (int percentileToRemove = currentSnowPercentile; percentileToRemove > newSnowPercentile || percentileToRemove == 0; percentileToRemove--) {
					removeSnowFromGround(percentileToRemove);
				}
			}
		}

		gameContext.getMapEnvironment().setFallenSnow(newSnow);
	}

	private void addSnowToGround(int percentileToAdd) {
		for (MapTile mapTile : gameContext.getAreaMap().getTilesForPercentile(percentileToAdd)) {
			if (mapTile.getRoof().getState().equals(TileRoofState.OPEN) && !mapTile.getFloor().getFloorType().equals(snowFloorType) &&
				!mapTile.hasWall() && !mapTile.getFloor().isRiverTile() & !mapTile.getFloor().hasBridge()) {
				messageDispatcher.dispatchMessage(MessageType.REPLACE_FLOOR, new ReplaceFloorMessage(mapTile.getTilePosition(), snowFloorType, snowMaterialType));
			}
		}
	}

	private void removeSnowFromGround(int percentileToRemove) {
		for (MapTile mapTile : gameContext.getAreaMap().getTilesForPercentile(percentileToRemove)) {
			if (mapTile.getFloor().getFloorType().equals(snowFloorType)) {
				messageDispatcher.dispatchMessage(MessageType.UNDO_REPLACE_FLOOR, mapTile.getTilePosition());
			}
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
		timeToNextLightningStrike = null;
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

	private static final double MIN_SNOW_TO_TRIGGER_PERCENTILE = 0.3;

	private static final double MAX_SNOW_TO_TRIGGER_PERCENTILE = 0.7;
	private int toSnowPercentile(double snowAmount) {
		snowAmount -= MIN_SNOW_TO_TRIGGER_PERCENTILE;
		if (snowAmount < 0) {
			return 0;
		} else {
			return Math.min((int) (snowAmount / ((MAX_SNOW_TO_TRIGGER_PERCENTILE - MIN_SNOW_TO_TRIGGER_PERCENTILE) / 100.0)), 100);
		}
	}

	private void triggerLightningStrike() {
		List<MapTile> potentialStrikeLocations = new ArrayList<>();
		TiledMap map = gameContext.getAreaMap();
		for (int i = 0; i < 40; i++) {
			potentialStrikeLocations.add(map.getTile(
					gameContext.getRandom().nextInt(map.getWidth()),
					gameContext.getRandom().nextInt(map.getHeight()
			)));
		}

		Optional<MapTile> strikeLocation = potentialStrikeLocations.stream()
				.filter(tile -> tile.getRoof().getState().equals(TileRoofState.OPEN) && tile.getExploration().equals(TileExploration.EXPLORED))
				.sorted((a, b) -> strikeChance(b) - strikeChance(a))
				.findFirst();

		if (strikeLocation.isPresent()) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
					lightningEffectType, Optional.empty(), Optional.of(new JobTarget(strikeLocation.get())), (p) -> {}
			));
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
					thunderCrackSoundAsset, null, strikeLocation.get().getWorldPositionOfCenter(), null
			));

			if (gameContext.getRandom().nextBoolean()) {
				messageDispatcher.dispatchMessage(MessageType.START_FIRE_IN_TILE, strikeLocation.get());
			}
		}

	}

	private int strikeChance(MapTile a) {
		if (a.hasTree()) {
			return 100;
		} else {
			return a.getEntities().size();
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
