package technology.rocketjump.undermount.entities.ai.goap.actions;

import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
@ProvidedBy(ActionDictionaryProvider.class)
public class ActionDictionary {

	private Map<String, Class<? extends Action>> byName = new HashMap<>();

	public ActionDictionary(Set<Class<? extends Action>> actionClasses) {
		for (Class<? extends Action> actionClass : actionClasses) {
			String className = actionClass.getSimpleName().replace("Action", "");
			byName.put(className, actionClass);
		}
	}

	public Class<? extends Action> getByName(String name) {
		Class<? extends Action> actionClass = byName.get(name);
		if (actionClass == null) {
			throw new RuntimeException("Could not find entity Action with name: " + name);
		}
		return actionClass;
	}

}
