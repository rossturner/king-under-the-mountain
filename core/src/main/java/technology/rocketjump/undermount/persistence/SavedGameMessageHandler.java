package technology.rocketjump.undermount.persistence;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.AssetDisposable;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.gamecontext.GameContextFactory;
import technology.rocketjump.undermount.gamecontext.GameContextRegister;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.logging.CrashHandler;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.ThreadSafeMessageDispatcher;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskManager;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskResult;
import technology.rocketjump.undermount.messaging.async.ErrorType;
import technology.rocketjump.undermount.messaging.types.GameSaveMessage;
import technology.rocketjump.undermount.modding.LocalModRepository;
import technology.rocketjump.undermount.modding.model.ParsedMod;
import technology.rocketjump.undermount.persistence.model.InvaidSaveOrModsMissingException;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.constructions.Construction;
import technology.rocketjump.undermount.screens.menus.MenuType;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.widgets.GameDialogDictionary;
import technology.rocketjump.undermount.ui.widgets.ModalDialog;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.persistence.SavedGameStore.ARCHIVE_HEADER_ENTRY_NAME;

@Singleton
public class SavedGameMessageHandler implements Telegraph, GameContextAware, AssetDisposable {

	private final SavedGameDependentDictionaries relatedStores;
	private final MessageDispatcher messageDispatcher;
	private final UserFileManager userFileManager;
	private final BackgroundTaskManager backgroundTaskManager;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final GameContextRegister gameContextRegister;
	private final GameContextFactory gameContextFactory;
	private final LocalModRepository localModRepository;
	private final GameDialogDictionary gameDialogDictionary;
	private final ConstantsRepo constantsRepo;
	private final SavedGameStore savedGameStore;
	private final I18nTranslator i18nTranslator;
	private GameContext gameContext;

	private boolean savingInProgress;
	private boolean disposed;

	@Inject
	public SavedGameMessageHandler(SavedGameDependentDictionaries savedGameDependentDictionaries,
								   MessageDispatcher messageDispatcher, UserFileManager userFileManager,
								   BackgroundTaskManager backgroundTaskManager, PrimaryCameraWrapper primaryCameraWrapper,
								   GameContextRegister gameContextRegister, GameContextFactory gameContextFactory,
								   LocalModRepository localModRepository, GameDialogDictionary gameDialogDictionary,
								   ConstantsRepo constantsRepo, SavedGameStore savedGameStore, I18nTranslator i18nTranslator) {
		this.relatedStores = savedGameDependentDictionaries;
		this.messageDispatcher = messageDispatcher;
		this.userFileManager = userFileManager;
		this.backgroundTaskManager = backgroundTaskManager;
		this.primaryCameraWrapper = primaryCameraWrapper;
		this.gameContextRegister = gameContextRegister;
		this.gameContextFactory = gameContextFactory;
		this.localModRepository = localModRepository;
		this.gameDialogDictionary = gameDialogDictionary;
		this.constantsRepo = constantsRepo;
		this.savedGameStore = savedGameStore;
		this.i18nTranslator = i18nTranslator;

		messageDispatcher.addListener(this, MessageType.REQUEST_SAVE);
		messageDispatcher.addListener(this, MessageType.PERFORM_LOAD);
		messageDispatcher.addListener(this, MessageType.PERFORM_SAVE);
		messageDispatcher.addListener(this, MessageType.TRIGGER_QUICKLOAD);
		messageDispatcher.addListener(this, MessageType.SAVE_COMPLETED);
		messageDispatcher.addListener(this, MessageType.DAY_ELAPSED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.REQUEST_SAVE: {
				triggerSaveProcess();
				return true;
			}
			case MessageType.DAY_ELAPSED: {
				// TODO configurable autosaving such as per-day, per-season, per-year, off
				triggerSaveProcess();
				return true;
			}
			case MessageType.PERFORM_SAVE: {
				GameSaveMessage message = (GameSaveMessage) msg.extraInfo;
				if (gameContext != null) {
					try {
						save(gameContext.getSettlementState().getSettlementName(), message.asynchronous);
					} catch (Exception e) {
						savingInProgress = false;
						messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.WHILE_SAVING);
						CrashHandler.logCrash(e);
					}
				}
				return true;
			}
			case MessageType.PERFORM_LOAD: {
				SavedGameInfo savedGameInfo = (SavedGameInfo) msg.extraInfo;
				try {
					load(savedGameInfo);
					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
				} catch (FileNotFoundException e) {
					// Mostly ignoring file not found errors
					Logger.warn(e.getMessage());
				} catch (InvaidSaveOrModsMissingException e) {
					ModalDialog dialog = gameDialogDictionary.createModsMissingSaveExceptionDialog(e.missingModNames);
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
					Logger.warn(e);
				} catch (InvalidSaveException e) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.INVALID_SAVE_FILE);
					Logger.warn(e);
				} catch (Exception e) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.WHILE_LOADING);
					CrashHandler.logCrash(e);
				}
				messageDispatcher.dispatchMessage(0.05f, MessageType.HIDE_AUTOSAVE_PROMPT);
				return true;
			}
			case MessageType.TRIGGER_QUICKLOAD: {
				PersistenceCallback callback = (PersistenceCallback) msg.extraInfo;
				Optional<SavedGameInfo> latest = savedGameStore.getLatest();
				boolean loadSuccess = false;
				try {
					if (latest.isPresent()) {
						load(latest.get());
						messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
						loadSuccess = true;
					}
				} catch (FileNotFoundException e) {
					// Mostly ignoring file not found errors
					Logger.warn(e.getMessage());
				} catch (InvaidSaveOrModsMissingException e) {
					ModalDialog dialog = gameDialogDictionary.createModsMissingSaveExceptionDialog(e.missingModNames);
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
					Logger.warn(e);
				} catch (InvalidSaveException e) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.INVALID_SAVE_FILE);
					Logger.warn(e);
				} catch (Exception e) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_ERROR, ErrorType.WHILE_LOADING);
					CrashHandler.logCrash(e);
				} finally {
					if (callback != null) {
						callback.gameLoadAttempt(loadSuccess);
					}
				}
				return true;
			}
			case MessageType.SAVE_COMPLETED: {
				savingInProgress = false;
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void triggerSaveProcess() {
		messageDispatcher.dispatchMessage(MessageType.SHOW_AUTOSAVE_PROMPT);
		messageDispatcher.dispatchMessage(0.01f, MessageType.PERFORM_SAVE, new GameSaveMessage(true));
		messageDispatcher.dispatchMessage(0.05f, MessageType.HIDE_AUTOSAVE_PROMPT);
	}

	public void save(String saveFileName, boolean asynchronous) throws Exception {
		if (savingInProgress || disposed) {
			return;
		} else {
			savingInProgress = true;
		}
		backgroundTaskManager.waitForOutstandingTasks();

		SavedGameStateHolder stateHolder = new SavedGameStateHolder();

		for (GameMaterial dynamicMaterial : gameContext.getDynamicallyCreatedMaterialsByCombinedId().values()) {
			dynamicMaterial.writeTo(stateHolder);
		}
		for (Job job : gameContext.getJobs().values()) {
			job.writeTo(stateHolder);
		}
		for (Entity entity : gameContext.getEntities().values()) {
			entity.writeTo(stateHolder);
			stateHolder.entityIdsToLoad.add(entity.getId());
		}
		for (Construction construction : gameContext.getConstructions().values()) {
			construction.writeTo(stateHolder);
		}
		for (Room room : gameContext.getRooms().values()) {
			room.writeTo(stateHolder);
		}
		TiledMap map = gameContext.getAreaMap();
		stateHolder.mapJson.put("seed", map.getSeed());
		stateHolder.mapJson.put("width", map.getWidth());
		stateHolder.mapJson.put("height", map.getHeight());
		stateHolder.mapJson.put("embarkPoint", JSONUtils.toJSON(map.getEmbarkPoint()));
		stateHolder.mapJson.put("numRegions", map.getNumRegions());
		stateHolder.mapJson.put("defaultFloor", map.getDefaultFloor().getFloorTypeName());
		stateHolder.mapJson.put("defaultFloorMaterial", map.getDefaultFloorMaterial().getMaterialName());

		for (int y = 0; y < map.getHeight(); y++) {
			for (int x = 0; x < map.getWidth(); x++) {
				map.getTile(x, y).writeTo(stateHolder);
			}
		}
		for (int y = 0; y <= map.getHeight(); y++) {
			for (int x = 0; x <= map.getWidth(); x++) {
				map.getVertex(x, y).writeTo(stateHolder);
			}
		}

		gameContext.getSettlementState().writeTo(stateHolder);
		((ThreadSafeMessageDispatcher) messageDispatcher).writeTo(stateHolder);
		gameContext.getGameClock().writeTo(stateHolder);
		stateHolder.setSequentialIdPointer(SequentialIdGenerator.lastId());
		primaryCameraWrapper.writeTo(stateHolder);
		stateHolder.setActiveMods(localModRepository.getActiveMods());

		JSONObject fileContents = stateHolder.toCombinedJson();

		Callable<BackgroundTaskResult> writeToDisk = () -> {
			try {
				JSONObject headerJson = produceHeaderFrom(fileContents);

				File saveFile = userFileManager.getOrCreateSaveFile(saveFileName);
				File tempMainFile = userFileManager.getOrCreateSaveFile("body.temp");
				File tempHeaderFile = userFileManager.getOrCreateSaveFile("header.temp");
				writeJsonToFile(fileContents, tempMainFile);
				writeJsonToFile(headerJson, tempHeaderFile);

				OutputStream archiveStream = new FileOutputStream(saveFile);
				ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);

				addArchiveEntry(tempHeaderFile, ARCHIVE_HEADER_ENTRY_NAME, archive);
				addArchiveEntry(tempMainFile, saveFileName + ".json", archive);

				archive.finish();
				IOUtils.closeQuietly(archiveStream);

				tempMainFile.delete();
				tempHeaderFile.delete();
				messageDispatcher.dispatchMessage(MessageType.SAVE_COMPLETED, new SavedGameInfo(saveFile, headerJson, i18nTranslator));
				return BackgroundTaskResult.success();
			} catch (Exception e) {
				CrashHandler.logCrash(e);
				messageDispatcher.dispatchMessage(MessageType.SAVE_COMPLETED);
				return BackgroundTaskResult.error(ErrorType.WHILE_SAVING);
			}
		};

		if (asynchronous) {
			backgroundTaskManager.runTask(writeToDisk);
		} else {
			writeToDisk.call();
		}
	}

	private void writeJsonToFile(JSONObject json, File targetFile) throws IOException {
		targetFile.delete();
		targetFile.createNewFile();
		BufferedWriter tempFileWriter = new BufferedWriter(new FileWriter(targetFile));
		try {
			JSON.writeJSONStringTo(json, tempFileWriter,
					SerializerFeature.DisableCircularReferenceDetect);
		} finally {
			IOUtils.closeQuietly(tempFileWriter);
		}
	}

	private JSONObject produceHeaderFrom(JSONObject mainJsonContent) {
		JSONObject headerJson = new JSONObject(true);
		headerJson.put("name", mainJsonContent.getJSONObject("settlementState").getString("settlementName"));
		headerJson.put("version", mainJsonContent.getString("version"));
		headerJson.put("mods", mainJsonContent.getJSONObject("mods"));
		headerJson.put("clock", mainJsonContent.getJSONObject("clock"));
		return headerJson;
	}

	private void addArchiveEntry(File sourceFile, String entryName, ArchiveOutputStream archive) throws IOException {
		ZipArchiveEntry headerEntry = new ZipArchiveEntry(entryName);
		archive.putArchiveEntry(headerEntry);
		BufferedInputStream headerInputStream = new BufferedInputStream(new FileInputStream(sourceFile));
		IOUtils.copy(headerInputStream, archive);
		IOUtils.closeQuietly(headerInputStream);
		archive.closeArchiveEntry();
	}


	public void load(SavedGameInfo savedGameInfo) throws IOException, InvalidSaveException, ArchiveException {
		if (savingInProgress) {
			return;
		}
		File saveFile = userFileManager.getSaveFile(savedGameInfo);
		if (saveFile == null) {
			throw new FileNotFoundException("Save file does not exist: " + savedGameInfo.file.getName() + ".save");
		}

		InputStream archiveStream = new FileInputStream(saveFile);
		ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, archiveStream);
		ArchiveEntry archiveEntry = archive.getNextEntry();
		while (archiveEntry != null && archiveEntry.getName().equals(ARCHIVE_HEADER_ENTRY_NAME)) {
			archiveEntry = archive.getNextEntry();
		}

		if (archiveEntry == null) {
			throw new IOException("Could not find main entry in " + saveFile.getName());
		}

		StringWriter stringWriter = new StringWriter();
		IOUtils.copy(archive, stringWriter);
		IOUtils.closeQuietly(stringWriter);
		IOUtils.closeQuietly(archive);
		IOUtils.closeQuietly(archiveStream);

		JSONObject storedJson = JSON.parseObject(stringWriter.toString());
		SavedGameStateHolder stateHolder = new SavedGameStateHolder(storedJson);
		try {
			stateHolder.jsonToObjects(relatedStores);
		} catch (InvalidSaveException e) {
			List<String> missingModNames = getMissingModNames(stateHolder);
			if (!missingModNames.isEmpty()) {
				throw new InvaidSaveOrModsMissingException(missingModNames, e.getMessage());
			} else {
				throw e;
			}
		}

		GameContext gameContext = gameContextFactory.create(stateHolder);
		// Need constant before initialising entities
		gameContext.setConstantsRepo(constantsRepo);
		for (Entity entity : stateHolder.entities.values()) {
			entity.init(messageDispatcher, gameContext);
		}
		TiledMap map = gameContext.getAreaMap();
		for (int y = 0; y < map.getHeight(); y++) {
			for (int x = 0; x < map.getWidth(); x++) {
				MapTile mapTile = map.getTile(x, y);
				for (Long entityId : mapTile.getEntityIds()) {
					Entity entity = stateHolder.entities.get(entityId);
					mapTile.addEntity(entity);
				}
			}
		}


		((ThreadSafeMessageDispatcher) messageDispatcher).readFrom(null, stateHolder, relatedStores);

		gameContextRegister.setNewContext(gameContext);
		primaryCameraWrapper.readFrom(stateHolder.cameraJson, stateHolder, relatedStores);

		List<String> missingMods = getMissingModNames(stateHolder);
		if (!missingMods.isEmpty()) {
			ModalDialog modsMissingDialog = gameDialogDictionary.createModsMissingDialog(missingMods);
			messageDispatcher.dispatchMessage(0.1f, MessageType.SHOW_DIALOG, modsMissingDialog);
		}
	}

	private List<String> getMissingModNames(SavedGameStateHolder savedGameStateHolder) {
		List<ParsedMod> currentlyActiveMods = localModRepository.getActiveMods();
		List<String> currentlyActiveModNames = currentlyActiveMods.stream().map(mod -> mod.getInfo().getName()).collect(Collectors.toList());
		Set<String> saveFileModNames = savedGameStateHolder.activeModNamesToVersions.keySet();

		return saveFileModNames.stream()
				.filter(saveModName -> !currentlyActiveModNames.contains(saveModName))
				.collect(Collectors.toList());

	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.gameContext = null;
	}

	@Override
	public void dispose() {
		this.disposed = true;
	}
}
