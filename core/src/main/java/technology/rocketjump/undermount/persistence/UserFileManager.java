package technology.rocketjump.undermount.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.pmw.tinylog.Logger;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class UserFileManager {

	File userFileDirectoryForGame;
	File saveGameDirectory;

	@Inject
	public UserFileManager() {
		try {
			File defaultDocsDir = FileSystemView.getFileSystemView().getDefaultDirectory();
			userFileDirectoryForGame = initDirectory(defaultDocsDir.toPath().resolve("King under the Mountain").toFile());
		} catch (Exception e) {
			// Couldn't write to user dir
			Logger.error(e);
			// Just write files local to game for now
			userFileDirectoryForGame = new File(".");
		}
	}

	public void initSaveDir(String saveDirPath) throws IOException {
		saveGameDirectory = initDirectory(saveDirPath);
	}

	public File getSaveFile(SavedGameInfo info) {
		return getOrCreateFile(info.file, false);
	}
	
	public List<File> getAllSaveFiles() {
		return Arrays.stream(saveGameDirectory.listFiles())
				.filter(file -> FilenameUtils.getExtension(file.getName()).equals("save"))
				.collect(Collectors.toList());
	}

	public List<File> getAllSaveDirectories() {
		return Arrays.stream(saveGameDirectory.listFiles())
				.filter(File::isDirectory)
				.collect(Collectors.toList());
	}

	public File getOrCreateSaveFile(String filename) {
		File file = new File(saveGameDirectory.getAbsolutePath(), filename + ".save");
		return getOrCreateFile(file, true);
	}

	public File getOrCreateFile(String filename) {
		File file = new File(userFileDirectoryForGame.getAbsolutePath(), filename);
		return getOrCreateFile(file, true);
	}

	private File getOrCreateFile(File file, boolean createIfNotExists) {
		try {
			if (!file.exists()) {
				if (createIfNotExists) {
					file.createNewFile();
				} else {
					return null;
				}
			}
			return file;
		} catch (IOException e) {
			Logger.error("Could not create file " + file.getAbsolutePath());
			throw new RuntimeException(e);
		}
	}

	public String readClientId() {
		File clientFile = getOrCreateFile("client");
		String clientId;
		try {
			clientId = FileUtils.readFileToString(clientFile);
			if (clientId.length() <= 0) {
				clientId = randomUuid();
				FileUtils.write(clientFile, clientId);
			}
		} catch (IOException e) {
			Logger.error("Failed to read/write client ID");
			clientId = "unknown";
		}
		return clientId;
	}

	private File initDirectory(String pathToDir) throws IOException {
		return initDirectory(new File(pathToDir));
	}

	private File initDirectory(File gameDirectory) throws IOException {
		if (!gameDirectory.exists()) {
			FileUtils.forceMkdir(gameDirectory);
		}

		// Attempt to create and delete test file
		File testFile = new File(gameDirectory.getAbsolutePath(), "test.txt");

		boolean created = testFile.createNewFile();
		boolean deleted = testFile.delete();

		return gameDirectory;
	}

	private static String randomUuid() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}
}
