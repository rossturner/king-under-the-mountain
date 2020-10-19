package technology.rocketjump.undermount.entities.model;

import technology.rocketjump.undermount.entities.components.EntityComponent;

import java.util.*;

public class EntityComponentMap {

	private Map<Class, Object> componentMap = new HashMap<>();

	public <T extends EntityComponent> void add(T component) {
		componentMap.put(component.getClass(), component);
	}

	public <T extends EntityComponent> T remove(Class<T> classType) {
		Object removed = componentMap.remove(classType);
		if (removed == null) {
			return null;
		} else {
			return (T)removed;
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends EntityComponent> T get(Class<T> classType) {
		Object instance = componentMap.get(classType);
		if (instance == null) {
			return null;
		} else {
			return (T)instance;
		}
	}

	public <T extends EntityComponent> List<T> values() {
		List<T> allComponents = new LinkedList<>();
		for (Object value : componentMap.values()) {
			allComponents.add((T)value);
		}
		return allComponents;
	}

	public Set<Class> keySet() {
		return componentMap.keySet();
	}
}
