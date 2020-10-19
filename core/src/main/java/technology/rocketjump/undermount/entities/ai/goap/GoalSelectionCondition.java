package technology.rocketjump.undermount.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.undermount.environment.GameClock;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = GoalSelectionByMemory.class, name = "MEMORY"),
		@JsonSubTypes.Type(value = GoalSelectionByNeed.class, name = "NEED"),
})
public interface GoalSelectionCondition {

	@JsonIgnore
	boolean apply(GameClock gameClock, NeedsComponent needsComponent, MemoryComponent memoryComponent);

}
