package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.google.inject.Provider;
import org.reflections.Reflections;

import java.util.Set;

public class ActionDictionaryProvider implements Provider<ActionDictionary> {

	@Override
	public ActionDictionary get() {
		// This kind of reflection takes ~0.3 seconds, which should be fine as it's once on startup
		Reflections reflections = new Reflections("technology.rocketjump.undermount.entities.ai.goap.actions");
		Set<Class<? extends Action>> actionTypes = reflections.getSubTypesOf(Action.class);
		return new ActionDictionary(actionTypes);
	}
}
