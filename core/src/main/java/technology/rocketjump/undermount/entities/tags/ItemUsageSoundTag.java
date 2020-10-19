package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class ItemUsageSoundTag extends Tag {

	private SoundAsset soundAsset;

	@Override
	public String getTagName() {
		return "USAGE_SOUND";
	}

	@Override
	public boolean isValid() {
		return args.size() == 1;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.soundAsset = tagProcessingUtils.soundAssetDictionary.getByName(args.get(0));
		if (this.soundAsset == null) {
			Logger.error("Could not find sound asset with name " + args.get(0) + " for " + getTagName());
		}
	}

	public SoundAsset getSoundAsset() {
		return soundAsset;
	}
}
