package technology.rocketjump.undermount.persistence;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SavedGameInfo {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

	public final File file;
	public final String settlementName;
	public final String version;
	public final Instant lastModifiedTime;
	public final String formattedFileModifiedTime;
	public final String formattedGameTime;

	public SavedGameInfo(File saveFile, JSONObject headerJson, I18nTranslator i18nTranslator) throws InvalidSaveException, IOException {
		GameClock clock = new GameClock();
		clock.readFrom(headerJson.getJSONObject("clock"), null, null);

		this.file = saveFile;
		this.settlementName = headerJson.getString("name");
		this.version = headerJson.getString("version");
		this.lastModifiedTime = Files.getLastModifiedTime(file.toPath()).toInstant();
		this.formattedFileModifiedTime = DATE_TIME_FORMATTER.format(lastModifiedTime);
		this.formattedGameTime = i18nTranslator.getDateTimeString(clock).toString();
	}

	public SavedGameInfo(File file, String settlementName, String version, String formattedGameTime) throws IOException {
		this.file = file;
		this.settlementName = settlementName;
		this.version = version;
		this.lastModifiedTime = Files.getLastModifiedTime(file.toPath()).toInstant();
		this.formattedFileModifiedTime = DATE_TIME_FORMATTER.format(lastModifiedTime);
		this.formattedGameTime = formattedGameTime;
	}

}
