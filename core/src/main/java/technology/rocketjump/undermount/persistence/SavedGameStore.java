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
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

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
	private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private boolean refreshInProgress = false;

	private final Map<String, SavedGameInfo> bySettlementName = new HashMap<>();

	@Inject
	public SavedGameStore(UserFileManager userFileManager, BackgroundTaskManager backgroundTaskManager,
						  MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator) {
		this.userFileManager = userFileManager;
		this.backgroundTaskManager = backgroundTaskManager;
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;

		messageDispatcher.addListener(this, MessageType.SAVED_GAMES_PARSED);
		messageDispatcher.addListener(this, MessageType.SAVE_COMPLETED);

		refresh();
	}

	public void refresh() {
		if (!refreshInProgress) {
			Logger.debug("Refreshing save file list");
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
								storedJson.getString("version"), lastModifiedTime, formattedLastModified, i18nTranslator.getDateTimeString(clock).toString()));
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
				Logger.debug("Save file list refreshed");
				this.refreshInProgress = false;
				bySettlementName.clear();
				for (SavedGameInfo savedGame : savedGames) {
					if (!bySettlementName.containsKey(savedGame.settlementName) ||
							bySettlementName.get(savedGame.settlementName).lastModifiedTime.isBefore(savedGame.lastModifiedTime)) {
						bySettlementName.put(savedGame.settlementName, savedGame);
					}
				}
				messageDispatcher.dispatchMessage(MessageType.SAVED_GAMES_LIST_UPDATED);
				return true;
			}
			case MessageType.SAVE_COMPLETED: {
				refresh();
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

	public Collection<SavedGameInfo> getAll() {
		return bySettlementName.values();
	}
}
