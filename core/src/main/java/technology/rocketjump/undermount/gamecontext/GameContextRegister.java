package technology.rocketjump.undermount.gamecontext;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.async.BackgroundTaskManager;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class keeps a handle to the current gamecontext of the game, for classes that need the map and other runtime-created objects
 */
@Singleton
public class GameContextRegister {

	private Map<String, GameContextAware> registered = new HashMap<>();
	private final BackgroundTaskManager backgroundTaskManager;

	private GameContext currentContext;

	@Inject
	public GameContextRegister(BackgroundTaskManager backgroundTaskManager) {
		this.backgroundTaskManager = backgroundTaskManager;
	}

	public void registerClasses(Set<Class<? extends GameContextAware>> implementations, Injector injector) {
		for (Class<? extends GameContextAware> implementationType : implementations) {
			if (!isRegistered(implementationType) && !implementationType.isInterface() && !Modifier.isAbstract(implementationType.getModifiers())) {
				register(injector.getInstance(implementationType));
			}
		}
	}

	public void setNewContext(GameContext newContext) {
		this.currentContext = newContext;

		backgroundTaskManager.clearContextRelatedState(); // outside of normal GameContextAware
		for (GameContextAware contextAware : registered.values()) {
			contextAware.clearContextRelatedState();
		}
		for (GameContextAware contextAware : registered.values()) {
			contextAware.onContextChange(currentContext);
		}
	}

	public void register(GameContextAware contextAwareInstance) {
		String className = contextAwareInstance.getClass().getName();
		if (registered.containsKey(className)) {
			throw new RuntimeException("Duplicate class registered in " + this.getClass().getName() + ": " + className);
		}
		registered.put(className, contextAwareInstance);
		if (currentContext != null) {
			contextAwareInstance.onContextChange(currentContext);
		}
	}

	private boolean isRegistered(Class<? extends GameContextAware> classType) {
		return registered.containsKey(classType.getName());
	}

}
