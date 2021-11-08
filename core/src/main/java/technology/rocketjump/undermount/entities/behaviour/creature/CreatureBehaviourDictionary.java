package technology.rocketjump.undermount.entities.behaviour.creature;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class CreatureBehaviourDictionary {

	private Map<String, Class<? extends BehaviourComponent>> byName = new HashMap<>();

	@Inject
	public CreatureBehaviourDictionary() {
		Reflections reflections = new Reflections(this.getClass().getPackage().getName(), new SubTypesScanner());
		Set<Class<? extends BehaviourComponent>> behaviourClasses = reflections.getSubTypesOf(BehaviourComponent.class);
		for (Class<? extends BehaviourComponent> behaviourClass : behaviourClasses) {
			if (!Modifier.isAbstract(behaviourClass.getModifiers())) {
				byName.put(behaviourClass.getSimpleName().substring(0, behaviourClass.getSimpleName().indexOf("Behaviour")), behaviourClass);
			}
		}
	}

	public Class<? extends BehaviourComponent> getByName(String name) {
		return byName.get(name);
	}

}
