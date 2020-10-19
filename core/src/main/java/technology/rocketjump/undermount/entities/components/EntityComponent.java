package technology.rocketjump.undermount.entities.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;

public interface EntityComponent extends ChildPersistable {

	EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext);

}
