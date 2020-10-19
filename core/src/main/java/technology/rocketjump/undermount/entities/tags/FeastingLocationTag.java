package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.furniture.FeastingLocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class FeastingLocationTag extends Tag {

	@Override
	public String getTagName() {
		return "FEASTING_LOCATION";
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		entity.addComponent(new FeastingLocationComponent());
	}

}
