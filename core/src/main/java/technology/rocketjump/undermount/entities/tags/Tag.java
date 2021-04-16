package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.rooms.Room;

import java.util.List;
import java.util.Objects;

public abstract class Tag {

	public abstract String getTagName();

	protected List<String> args;

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public List<String> getArgs() {
		return args;
	}

	public abstract boolean isValid(TagProcessingUtils tagProcessingUtils);

	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException(this.getClass().getSimpleName() + " does not apply to entities");
	}

	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		throw new NotImplementedException(this.getClass().getSimpleName() + " does not apply to rooms");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tag tag = (Tag) o;
		return this.getTagName().equals(tag.getTagName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getTagName());
	}
}
