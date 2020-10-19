package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.audio.model.SoundAsset;
import technology.rocketjump.undermount.audio.model.SoundAssetDictionary;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.RequestSoundMessage;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.settlement.notifications.Notification;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class NotificationGuiView implements GuiView, GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final IconButtonFactory iconButtonFactory;
	private final GameDialogDictionary gameDialogDictionary;
	private Table table;
	private GameContext gameContext;
	private SoundAsset receiveNotificationSound;
	private SoundAsset openNotificationSound;

	private final Map<Notification, IconButton> currentNotificationButtons = new LinkedHashMap<>();

	@Inject
	public NotificationGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							   IconButtonFactory iconButtonFactory, GameDialogDictionary gameDialogDictionary, SoundAssetDictionary soundAssetDictionary) {
		this.iconButtonFactory = iconButtonFactory;
		this.messageDispatcher = messageDispatcher;
		this.gameDialogDictionary = gameDialogDictionary;
		Skin uiSkin = guiSkinRepository.getDefault();
		this.receiveNotificationSound = soundAssetDictionary.getByName("NewNotification");
		this.openNotificationSound = soundAssetDictionary.getByName("NotificationOpen");

		table = new Table(uiSkin);
		table.pad(6);

		messageDispatcher.addListener(this, MessageType.POST_NOTIFICATION);
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(table).right();
	}

	@Override
	public void update() {
		if (gameContext != null) {
			table.clearChildren();

			for (IconButton iconButton : currentNotificationButtons.values()) {
				table.add(iconButton).pad(4).row();
			}
		}
	}

	@Override
	public GuiViewName getName() {
		// This is a special case GuiView which lives outside of the normal usage
		return null;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		for (Notification notification : gameContext.getSettlementState().queuedNotifications) {
			addIconButton(notification);
		}
		update();
	}

	@Override
	public void clearContextRelatedState() {
		currentNotificationButtons.clear();
		table.clearChildren();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.POST_NOTIFICATION: {
				Notification notification = (Notification) msg.extraInfo;

				SoundAsset notificationSound = receiveNotificationSound;
				if (notification.getType().getOverrideSoundAsset() != null) {
					notificationSound = notification.getType().getOverrideSoundAsset();
				}
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(notificationSound));

				if (GlobalSettings.PAUSE_FOR_NOTIFICATIONS) {
					if (!gameContext.getGameClock().isPaused()) {
						messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
						showNotification(notification);
					} else {
						// If already paused, queue up the notification
						add(notification);
					}
				} else {
					add(notification);
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void add(Notification notification) {
		addIconButton(notification);
		gameContext.getSettlementState().queuedNotifications.add(notification);
		update();
	}

	private void addIconButton(Notification notification) {
		IconButton notificationButton = iconButtonFactory.create(null, notification.getType().getIconName(),
				notification.getType().getIconColor(), ButtonStyle.HALF_SIZE_NO_TEXT);
		notificationButton.setOnClickSoundAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(openNotificationSound));
		});
		notificationButton.setAction(() -> {
			showNotification(notification);
		});
		notificationButton.setRightClickAction(() -> {
			removeNotification(notification);
		});
		currentNotificationButtons.put(notification, notificationButton);
	}

	private void showNotification(Notification notification) {
		removeNotification(notification);

		NotificationDialog notificationDialog = gameDialogDictionary.create(notification);
		messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, notificationDialog);
	}

	private void removeNotification(Notification notification) {
		currentNotificationButtons.remove(notification);
		gameContext.getSettlementState().queuedNotifications.remove(notification);
		update();
	}
}
