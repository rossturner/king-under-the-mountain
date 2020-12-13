package technology.rocketjump.undermount.persistence;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskManager;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskResult;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

@Singleton
public class SavedGameStore implements Telegraph, I18nUpdatable {

	public static final String ARCHIVE_HEADER_ENTRY_NAME = "header.json";
	private final UserFileManager userFileManager;
	private final BackgroundTaskManager backgroundTaskManager;
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
						InputStream archiveStream = new FileInputStream(saveFile);
						ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, archiveStream);
						ArchiveEntry archiveEntry = archive.getNextEntry();
						while (archiveEntry != null && !archiveEntry.getName().equals(ARCHIVE_HEADER_ENTRY_NAME)) {
							archiveEntry = archive.getNextEntry();
						}

						if (archiveEntry == null) {
							Logger.error("Could not find " + ARCHIVE_HEADER_ENTRY_NAME + " in " + saveFile.getName());
							continue;
						}

						StringWriter headerWriter = new StringWriter();
						IOUtils.copy(archive, headerWriter);
						IOUtils.closeQuietly(headerWriter);
						IOUtils.closeQuietly(archive);
						IOUtils.closeQuietly(archiveStream);

						JSONObject headerJson = JSON.parseObject(headerWriter.toString());

						GameClock clock = new GameClock();
						clock.readFrom(headerJson.getJSONObject("clock"), null, null);

						results.add(new SavedGameInfo(saveFile, headerJson.getString("name"),
								headerJson.getString("version"), i18nTranslator.getDateTimeString(clock).toString()));
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
				SavedGameInfo savedGameInfo = (SavedGameInfo) msg.extraInfo;
				if (savedGameInfo != null) {
					this.bySettlementName.put(savedGameInfo.settlementName, savedGameInfo);
				}
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

	public boolean hasSaveOrIsRefreshing() {
		return refreshInProgress || !bySettlementName.isEmpty();
	}

	@Override
	public void onLanguageUpdated() {
		// Game clock display requires translation
		refresh();
	}
}
