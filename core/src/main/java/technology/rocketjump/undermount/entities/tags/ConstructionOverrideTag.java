package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class ConstructionOverrideTag extends Tag {

	@Override
	public String getTagName() {
		return "CONSTRUCTION_OVERRIDE";
	}

	@Override
	public boolean isValid() {
		return EnumUtils.isValidEnum(ConstructionOverrideSetting.class, args.get(0));
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Do nothing, handled at FurnitureType level
	}

	public enum ConstructionOverrideSetting {

		DO_NOT_ALLOCATE,
		REQUIRES_EDIBLE_LIQUID

	}

}
