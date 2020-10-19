package technology.rocketjump.undermount.rooms.components;

import technology.rocketjump.undermount.rooms.components.behaviour.RoomBehaviourComponent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RoomComponentMap {

	private Map<Class<? extends RoomComponent>, RoomComponent> componentMap = new HashMap<>();
	private RoomBehaviourComponent behaviourComponent;

	public <T extends RoomComponent> void add(T component) {
		componentMap.put(component.getClass(), component);
		if (component instanceof RoomBehaviourComponent) {
			this.behaviourComponent = (RoomBehaviourComponent) component;
		}
	}

	public <T extends RoomComponent> void remove(Class<T> classType) {
		componentMap.remove(classType);
	}

	@SuppressWarnings("unchecked")
	public <T extends RoomComponent> T get(Class<T> classType) {
		RoomComponent instance = componentMap.get(classType);
		if (instance == null) {
			return null;
		} else {
			return (T)instance;
		}
	}

	public RoomBehaviourComponent getBehaviourComponent() {
		return behaviourComponent;
	}

	public Set<Class<? extends RoomComponent>> keySet() {
		return componentMap.keySet();
	}

	public Collection<RoomComponent> getAll() {
		return componentMap.values();
	}
}
