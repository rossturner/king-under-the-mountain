package technology.rocketjump.undermount.assets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.MessageType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class AssetDisposableRegister implements Telegraph {

	private final Map<String, AssetDisposable> registered = new HashMap<>();

	@Inject
	public AssetDisposableRegister(MessageDispatcher messageDispatcher) {
		messageDispatcher.addListener(this, MessageType.SHUTDOWN_IN_PROGRESS);
	}

	public void registerClasses(Set<Class<? extends AssetDisposable>> updatableClasses, Injector injector) {
		for (Class updatableClass : updatableClasses) {
			if (!updatableClass.isInterface()) {
				register((AssetDisposable)injector.getInstance(updatableClass));
			}
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SHUTDOWN_IN_PROGRESS: {
				for (AssetDisposable disposable : registered.values()) {
					disposable.dispose();
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void register(AssetDisposable disposableInstance) {
		String className = disposableInstance.getClass().getName();
		if (registered.containsKey(className)) {
			throw new RuntimeException("Duplicate class registered in " + this.getClass().getName() + ": " + className);
		}
		registered.put(className, disposableInstance);
	}

}
