package technology.rocketjump.undermount.rooms.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class RoomComponentDictionary {

	private final Map<String, Class<? extends RoomComponent>> simpleNameMap = new HashMap<>();

	@Inject
	public RoomComponentDictionary() {
		Reflections reflections = new Reflections("technology.rocketjump.undermount", new SubTypesScanner());
		Set<Class<? extends RoomComponent>> componentClasses = reflections.getSubTypesOf(RoomComponent.class);

		for (Class<? extends RoomComponent> componentClass : componentClasses) {
			if (simpleNameMap.containsKey(componentClass.getSimpleName())) {
				throw new RuntimeException("Duplicate RoomComponent class name: " + componentClass.getSimpleName());
			} else {
				simpleNameMap.put(componentClass.getSimpleName(), componentClass);
			}
		}
	}

	public Class<? extends RoomComponent> getByName(String className) {
		return simpleNameMap.get(className);
	}

}
