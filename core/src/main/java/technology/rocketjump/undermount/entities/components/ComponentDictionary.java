package technology.rocketjump.undermount.entities.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class ComponentDictionary {

	private Map<String, Class<? extends EntityComponent>> simpleNameMap = new HashMap<>();

	@Inject
	public ComponentDictionary() {
		Reflections reflections = new Reflections("technology.rocketjump.undermount", new SubTypesScanner());
		Set<Class<? extends EntityComponent>> componentClasses = reflections.getSubTypesOf(EntityComponent.class);

		for (Class<? extends EntityComponent> componentClass : componentClasses) {
			if (simpleNameMap.containsKey(componentClass.getSimpleName())) {
				throw new RuntimeException("Duplicate EntityComponent class name: " + componentClass.getSimpleName());
			} else {
				simpleNameMap.put(componentClass.getSimpleName(), componentClass);
			}
		}
	}

	public Class<? extends EntityComponent> getByName(String className) {
		return simpleNameMap.get(className);
	}
}
