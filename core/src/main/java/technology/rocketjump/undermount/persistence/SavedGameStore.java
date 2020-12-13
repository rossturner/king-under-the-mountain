package technology.rocketjump.undermount.persistence;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskManager;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskResult;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Singleton
public class SavedGameStore implements Telegraph {

	private final UserFileManager userFileManager;
	private final BackgroundTaskManager backgroundTaskManager;
	private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private final MessageDispatcher messageDispatcher;
	private boolean refreshInProgress = false;

	private final Map<String, SavedGameInfo> bySettlementName = new HashMap<>();

	@Inject
	public SavedGameStore(UserFileManager userFileManager, BackgroundTaskManager backgroundTaskManager, MessageDispatcher messageDispatcher) {
		this.userFileManager = userFileManager;
		this.backgroundTaskManager = backgroundTaskManager;
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, MessageType.SAVED_GAMES_PARSED);

		refresh();
	}

	public void refresh() {
		if (!refreshInProgress) {
			refreshInProgress = true;
			backgroundTaskManager.runTask(() -> {
				List<SavedGameInfo> results = new ArrayList<>();
				for (File saveFile : userFileManager.getAllSaveFiles()) {
					try {
						String jsonString = FileUtils.readFileToString(saveFile);
						JSONObject storedJson = JSON.parseObject(jsonString);
						SavedGameStateHolder stateHolder = new SavedGameStateHolder(storedJson);

						Instant lastModifiedTime = Files.getLastModifiedTime(saveFile.toPath()).toInstant();
						String formattedLastModified = DATE_TIME_FORMATTER.format(lastModifiedTime);

						GameClock clock = new GameClock();
						clock.readFrom(storedJson.getJSONObject("clock"), null, null);

						results.add(new SavedGameInfo(saveFile, stateHolder.settlementStateJson.getString("settlementName"),
								storedJson.getString("version"), lastModifiedTime, formattedLastModified, clock.getFormattedGameTime()));
					} catch (Exception e) {
						Logger.error("Error while reading " + saveFile.getAbsolutePath());
					}
				}
				return BackgroundTaskResult.success(MessageType.SAVED_GAMES_PARSED, results);
			});
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SAVED_GAMES_PARSED: {
				List<SavedGameInfo> savedGames = (List<SavedGameInfo>) msg.extraInfo;
				bySettlementName.clear();
				for (SavedGameInfo savedGame : savedGames) {
					bySettlementName.put(savedGame.settlementName, savedGame);
				}
				messageDispatcher.dispatchMessage(MessageType.SAVED_GAMES_LIST_UPDATED);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public SavedGameInfo getByName(String settlementName) {
		return bySettlementName.get(settlementName);
	}

	public Optional<SavedGameInfo> getLatest() {
		return bySettlementName.values().stream()
				.sorted((o1, o2) -> o2.lastModifiedTime.compareTo(o1.lastModifiedTime))
				.findFirst();
	}

	public int count() {
		return bySettlementName.keySet().size();
	}
}
