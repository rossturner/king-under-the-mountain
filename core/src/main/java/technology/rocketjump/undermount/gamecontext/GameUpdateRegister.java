package technology.rocketjump.undermount.gamecontext;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class GameUpdateRegister {

	private final Map<String, Updatable> registered = new HashMap<>();
	private final GameContextRegister gameContextRegister;


	@Inject
	public GameUpdateRegister(GameContextRegister gameContextRegister) {
		this.gameContextRegister = gameContextRegister;
	}

	public void registerClasses(Set<Class<? extends Updatable>> updatableClasses, Injector injector) {
		for (Class updatableClass : updatableClasses) {
			register((Updatable)injector.getInstance(updatableClass));
		}
	}

	public void update(float deltaTime, boolean isPaused) {
		for (Updatable updatable : registered.values()) {
			if (updatable.runWhilePaused() || !isPaused) {
				updatable.update(deltaTime);
			}
		}
	}

	private void register(Updatable updatableInstance) {
		String className = updatableInstance.getClass().getName();
		if (registered.containsKey(className)) {
			throw new RuntimeException("Duplicate class registered in " + this.getClass().getName() + ": " + className);
		}
		registered.put(className, updatableInstance);
		if (updatableInstance instanceof GameContextAware) {
			gameContextRegister.register((GameContextAware) updatableInstance);
		}
	}
}
