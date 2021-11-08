package technology.rocketjump.undermount.environment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.files.FileHandle;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.environment.model.Season;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

/**
 * This class is used to update and retrieve the current time of day and date in the game world
 */
public class GameClock implements Persistable {

	public final double HOURS_IN_DAY;
	public final double MINUTES_IN_HOUR;
	public final double REAL_TIME_SECONDS_PER_GAME_DAY;

	private double timeOfDay; // Current time measured in hours - 0 is midnight (morning)
	private double currentGameTime; // Overall counter in hours for elapsed game time
	private boolean paused;

	private int dayOfSeason;
	public final int DAYS_IN_SEASON;

	private int currentDayNumber;
	private Season currentSeason;
	private GameSpeed currentGameSpeed = GameSpeed.NORMAL;
	private int currentYear;

	public GameClock() {
		FileHandle settingsJsonFile = new FileHandle("assets/settings/timeAndDaySettings.json");
		JSONObject timeDaySettings = JSON.parseObject(settingsJsonFile.readString());

		HOURS_IN_DAY = timeDaySettings.getDouble("hoursInDay");
		MINUTES_IN_HOUR = timeDaySettings.getDouble("minutesInHour");
		REAL_TIME_SECONDS_PER_GAME_DAY = timeDaySettings.getDouble("realTimeSecondsPerDay");

		DAYS_IN_SEASON = timeDaySettings.getInteger("daysInSeason");

		currentSeason = Season.valueOf(timeDaySettings.getString("initialSeason"));
		dayOfSeason = timeDaySettings.getInteger("initialDayOfSeason");
		timeOfDay = timeDaySettings.getDouble("initialTimeOfDay");
		currentGameTime = timeOfDay;
		currentDayNumber = 1;
		currentYear = 1;
	}

	public double realTimeToGameHours(double deltaRealtimeSeconds) {
		return (deltaRealtimeSeconds / REAL_TIME_SECONDS_PER_GAME_DAY) * HOURS_IN_DAY;
	}

	public double gameHoursToRealTimeSeconds(double gameHours) {
		return (gameHours / HOURS_IN_DAY) * REAL_TIME_SECONDS_PER_GAME_DAY;
	}

	public double gameHoursAsNumSeasons(double elapsedGameHours) {
		double days = elapsedGameHours / HOURS_IN_DAY;
		return days / DAYS_IN_SEASON;
	}

	public void update(float deltaRealtimeSeconds, MessageDispatcher messageDispatcher) {
		double elapsedHours = realTimeToGameHours(deltaRealtimeSeconds);
		timeOfDay += elapsedHours;
		currentGameTime += elapsedHours;
		if (timeOfDay > HOURS_IN_DAY) {
			timeOfDay -= HOURS_IN_DAY;

			dayOfSeason++;
			currentDayNumber++;
			if (dayOfSeason > DAYS_IN_SEASON) {
				dayOfSeason = 1;
				currentSeason = currentSeason.getNext();
//				messageDispatcher.dispatchMessage(MessageType.SEASON_ELAPSED);
				if (currentSeason.equals(Season.SPRING)) {
					currentYear++;
					messageDispatcher.dispatchMessage(MessageType.YEAR_ELAPSED);
				}
			}
			messageDispatcher.dispatchMessage(MessageType.DAY_ELAPSED);
		}
	}

	public void forceYearChange(MessageDispatcher messageDispatcher) {
		dayOfSeason = 1;
		if (!currentSeason.equals(Season.SPRING)) {
			currentSeason = Season.SPRING;
		}
		currentYear++;
		messageDispatcher.dispatchMessage(MessageType.YEAR_ELAPSED);
	}

	public double getGameTimeInHours() {
		return timeOfDay;
	}

	private StringBuilder formatter = new StringBuilder();

	public String getFormattedGameTime() {
		formatter.setLength(0); // Clear the stringbuilder
		int hours = getHourOfDay();
		int tensOfMinutes = (int) Math.floor((timeOfDay % 1.0f) * MINUTES_IN_HOUR / 10);
		int minutes = tensOfMinutes * 10;

		if (hours < 10) {
			formatter.append(0);
		}
		formatter.append(hours).append(":");
		if (minutes < 10) {
			formatter.append(0);
		}
		formatter.append(minutes);
		return formatter.toString();
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public double getCurrentGameTime() {
		return currentGameTime;
	}

	public int getHourOfDay() {
		return (int) Math.floor(timeOfDay);
	}

	public float getSpeedMultiplier() {
		return currentGameSpeed.speedMultiplier;
	}

	public int getDayOfSeason() {
		return dayOfSeason;
	}

	public Season getCurrentSeason() {
		return currentSeason;
	}

	public int getCurrentYear() {
		return currentYear;
	}

	public GameSpeed getCurrentGameSpeed() {
		return currentGameSpeed;
	}

	public void setCurrentGameSpeed(GameSpeed currentGameSpeed) {
		this.currentGameSpeed = currentGameSpeed;
	}

	public int getCurrentDayNumber() {
		return currentDayNumber;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		JSONObject asJson = savedGameStateHolder.gameClockJson;

		asJson.put("timeOfDay", timeOfDay);
		asJson.put("currentGameTime", currentGameTime);
		if (paused) {
			asJson.put("paused", true);
		}
		asJson.put("dayOfSeason", dayOfSeason);
		asJson.put("currentDayNumber", currentDayNumber);
		asJson.put("currentSeason", currentSeason.name());
		asJson.put("currentYear", currentYear);
		asJson.put("gameSpeed", currentGameSpeed.name());

		savedGameStateHolder.setGameClock(this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.timeOfDay = asJson.getDoubleValue("timeOfDay");
		this.currentGameTime = asJson.getDoubleValue("currentGameTime");
		this.paused = asJson.getBooleanValue("paused");
		this.dayOfSeason = asJson.getIntValue("dayOfSeason");
		this.currentDayNumber = asJson.getIntValue("currentDayNumber");
		this.currentSeason = EnumParser.getEnumValue(asJson, "currentSeason", Season.class, null);
		this.currentYear = asJson.getIntValue("currentYear");
		this.currentGameSpeed = EnumParser.getEnumValue(asJson, "gameSpeed", GameSpeed.class, GameSpeed.NORMAL);
	}
}
