package technology.rocketjump.undermount.entities.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public interface ParentDependentEntityComponent extends EntityComponent {

	void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext);

}
