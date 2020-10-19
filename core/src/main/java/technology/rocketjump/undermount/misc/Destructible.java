package technology.rocketjump.undermount.misc;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public interface Destructible {

	void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext);

}
