package technology.rocketjump.undermount.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.components.furniture.SleepingPositionComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.entities.tags.TagProcessingUtils;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class BedSleepingPositionTag extends Tag {
	@Override
	public String getTagName() {
		return "BED_SLEEPING_POSITION";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return EnumUtils.isValidEnum(EntityAssetOrientation.class, args.get(0));
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		SleepingPositionComponent sleepingPositionComponent = entity.getOrCreateComponent(SleepingPositionComponent.class);
		sleepingPositionComponent.setSleepingOrientation(EntityAssetOrientation.valueOf(args.get(0)));
	}
}
