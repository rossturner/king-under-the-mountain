package technology.rocketjump.undermount.persistence;

import java.io.File;

public class SavedGameInfo {

	public final File file;
	public final String settlementName;
	public final String version;
	public final String formattedFileModifiedTime;
	public final String formattedGameTime;

	public SavedGameInfo(File file, String settlementName, String version, String formattedFileModifiedTime, String formattedGameTime) {
		this.file = file;
		this.settlementName = settlementName;
		this.version = version;
		this.formattedFileModifiedTime = formattedFileModifiedTime;
		this.formattedGameTime = formattedGameTime;
	}
}
