package technology.rocketjump.undermount.messaging.types;

import technology.rocketjump.undermount.audio.model.SoundAssetCallback;

public class RequestSoundAssetMessage {

	public final String assetName;
	public final SoundAssetCallback callback;

	public RequestSoundAssetMessage(String assetName, SoundAssetCallback callback) {
		this.assetName = assetName;
		this.callback = callback;
	}
}
