package technology.rocketjump.undermount.entities.ai.goap.actions;

import java.util.ArrayList;
import java.util.List;

public class ActionTransitions {

	public final List<Class<? extends Action>> onSuccess = new ArrayList<>();
	public final  List<Class<? extends Action>> onFailure = new ArrayList<>();

}
