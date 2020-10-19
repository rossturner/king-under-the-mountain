package technology.rocketjump.undermount.audio.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.settlement.notifications.NotificationType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class SoundAssetDictionary {

	private final Map<String, SoundAsset> byName = new HashMap<>();

	@Inject
	public SoundAssetDictionary() {
		File assetDefinitionsFile = new File("assets/sounds/soundAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<SoundAsset> assetList = objectMapper.readValue(FileUtils.readFileToString(assetDefinitionsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, SoundAsset.class));

			for (SoundAsset asset : assetList) {
				init(asset);
				byName.put(asset.getName(), asset);
			}
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}

		for (NotificationType notificationType : NotificationType.values()) {
			if (notificationType.getOverrideSoundAssetName() != null) {
				notificationType.setOverrideSoundAsset(getByName(notificationType.getOverrideSoundAssetName()));
				if (notificationType.getOverrideSoundAsset() == null) {
					Logger.error("Could not find sound asset named " + notificationType.getOverrideSoundAssetName() + " for notification " + notificationType.name());
				}
			}
		}

	}

	private void init(SoundAsset asset) {
		List<String> qualifiedFilenames = new ArrayList<>();
		for (String unqualifiedFilename : asset.getFilenames()) {
			String qualifiedFilename = "assets/sounds/data/" + unqualifiedFilename;
			if (new File(qualifiedFilename).exists()) {
				qualifiedFilenames.add(qualifiedFilename);
			} else {
				Logger.error("Could not find expected sound file at " + qualifiedFilename);
			}
		}
		asset.setFilenames(qualifiedFilenames);
	}

	public SoundAsset getByName(String name) {
		return byName.get(name);
	}
}
