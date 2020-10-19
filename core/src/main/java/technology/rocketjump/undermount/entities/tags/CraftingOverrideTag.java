package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class CraftingOverrideTag extends Tag {

	@Override
	public String getTagName() {
		return "CRAFTING_OVERRIDE";
	}

	@Override
	public boolean isValid() {
		return EnumUtils.isValidEnum(CraftingOverrideSetting.class, args.get(0));
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Do nothing, used in CraftingBehaviour
	}

	public boolean includes(CraftingOverrideSetting setting) {
		return args.contains(setting.name());
	}

	public enum CraftingOverrideSetting {

		DO_NOT_HAUL_OUTPUT

	}

}
