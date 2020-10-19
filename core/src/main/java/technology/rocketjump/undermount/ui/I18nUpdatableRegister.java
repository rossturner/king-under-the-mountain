package technology.rocketjump.undermount.ui;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.materials.GameMaterialI18nUpdater;
import technology.rocketjump.undermount.messaging.InfoType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class keeps track of all I18nUpdateable implementations for notifying of language changes
 */
@Singleton
public class I18nUpdatableRegister implements Telegraph {

	private final GameMaterialI18nUpdater gameMaterialI18nUpdater;
	private final Map<String, I18nUpdatable> registered = new HashMap<>();
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;

	@Inject
	public I18nUpdatableRegister(MessageDispatcher messageDispatcher, GameMaterialI18nUpdater gameMaterialI18nUpdater, I18nTranslator i18nTranslator) {
		this.gameMaterialI18nUpdater = gameMaterialI18nUpdater;
		this.i18nTranslator = i18nTranslator;
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, MessageType.LANGUAGE_CHANGED);
	}

	public void registerClasses(Set<Class<? extends I18nUpdatable>> updatableClasses, Injector injector) {
		for (Class updatableClass : updatableClasses) {
			if (!updatableClass.isInterface()) {
				register((I18nUpdatable)injector.getInstance(updatableClass));
			}
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.LANGUAGE_CHANGED: {
				// Add any PRE-LANGUAGE CHANGED stuff here
				i18nTranslator.onLanguageUpdated();
				gameMaterialI18nUpdater.onLanguageUpdated();
				// Then the onLanguageUpdated() callbacks are called
				for (I18nUpdatable i18nUpdatable : registered.values()) {
					i18nUpdatable.onLanguageUpdated();
				}

				// Post-language changed
				if (!i18nTranslator.getDictionary().isCompleteTranslation()) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_INFO, InfoType.LANGUAGE_TRANSLATION_INCOMPLETE);
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void register(I18nUpdatable updatableInstance) {
		String className = updatableInstance.getClass().getName();
		if (registered.containsKey(className)) {
			throw new RuntimeException("Duplicate class registered in " + this.getClass().getName() + ": " + className);
		}
		registered.put(className, updatableInstance);
	}

}
