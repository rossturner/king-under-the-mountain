package technology.rocketjump.undermount.misc.twitch;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.planning.BackgroundTaskManager;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.undermount.misc.twitch.model.TwitchToken;
import technology.rocketjump.undermount.misc.twitch.model.TwitchViewer;
import technology.rocketjump.undermount.misc.twitch.tasks.GetTwitcAccountInfo;
import technology.rocketjump.undermount.misc.twitch.tasks.GetTwitchAuthToken;
import technology.rocketjump.undermount.misc.twitch.tasks.GetTwitchSubscribers;
import technology.rocketjump.undermount.misc.twitch.tasks.GetTwitchViewers;
import technology.rocketjump.undermount.persistence.UserPreferences;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This class lives outside of the usual Updateable instance so that it also runs when the main menu/options is showing
 */
@Singleton
public class TwitchTaskRunner {

	private static final float FREQUENT_UPDATE_INTERVAL_SECONDS = 0.51111f;
	private static final float INFREQUENT_UPDATE_INTERVAL_SECONDS = 30.7f;
	private final BackgroundTaskManager backgroundTaskManager;
	private final UserPreferences userPreferences;
	private final MessageDispatcher messageDispatcher;
	private final TwitchDataStore twitchDataStore;

	private Future<TwitchToken> getAuthTokenFuture;
	private Future<TwitchAccountInfo> getAccountInfo;
	private Future<List<TwitchViewer>> getViewers;
	private Future<List<TwitchViewer>> getSubscribers;

	private float timeSinceFrequentUpdate = 0f;
	private float timeSinceInfrequentUpdate = 0f;

	@Inject
	public TwitchTaskRunner(BackgroundTaskManager backgroundTaskManager, UserPreferences userPreferences,
							MessageDispatcher messageDispatcher, TwitchDataStore twitchDataStore) {
		this.backgroundTaskManager = backgroundTaskManager;
		this.userPreferences = userPreferences;
		this.messageDispatcher = messageDispatcher;
		this.twitchDataStore = twitchDataStore;

		if (twitchDataStore.getCurrentToken() != null) {
			updateUserInfo();
		}
	}

	public void authenticateWithCode(String code) {
		if (getAuthTokenFuture == null) {
			getAuthTokenFuture = backgroundTaskManager.postUntrackedCallable(new GetTwitchAuthToken(code));
		} else {
			Logger.warn("There is already a pending request for " + GetTwitchAuthToken.class.getSimpleName());
		}
	}

	public void updateUserInfo() {
		if (getAccountInfo == null) {
			getAccountInfo = backgroundTaskManager.postUntrackedCallable(new GetTwitcAccountInfo(twitchDataStore));
		}
	}

	public void update(float deltaTime) {
		timeSinceFrequentUpdate += deltaTime;
		timeSinceInfrequentUpdate += deltaTime;
		// TODO disable if twitch integration not set up or disabled
		if (timeSinceFrequentUpdate > FREQUENT_UPDATE_INTERVAL_SECONDS) {
			doFrequentUpdate();
		}
		if (timeSinceInfrequentUpdate > INFREQUENT_UPDATE_INTERVAL_SECONDS) {
			doInfrequentUpdate();
		}
	}

	private void doFrequentUpdate() {
		timeSinceFrequentUpdate = 0f;

		if (getAuthTokenFuture != null) {
			if (getAuthTokenFuture.isDone()) {
				try {
					TwitchToken twitchToken = getAuthTokenFuture.get();
					twitchDataStore.setCurrentToken(twitchToken);
					updateUserInfo();
				} catch (InterruptedException | ExecutionException e) {
					Logger.error("Error with " + GetTwitchAuthToken.class.getSimpleName() + ", " + e.getMessage());
					messageDispatcher.dispatchMessage(MessageType.TWITCH_AUTH_CODE_FAILURE);
				}
				getAuthTokenFuture = null;
			}
		}

		if (getAccountInfo != null) {
			if (getAccountInfo.isDone()) {
				try {
					TwitchAccountInfo twitchAccountInfo = getAccountInfo.get();
					twitchDataStore.setAccountInfo(twitchAccountInfo);
					doInfrequentUpdate();
				} catch (Exception e) {
					Logger.error(e.getMessage());
					// Failed to update account info so refresh token must have expired
					twitchDataStore.setCurrentToken(null);
					twitchDataStore.setAccountInfo(null);
				}
				getAccountInfo = null;
			}
		}

		if (getViewers != null) {
			if (getViewers.isDone()) {
				try {
					twitchDataStore.setCurrentViewers(getViewers.get());
				} catch (InterruptedException | ExecutionException e) {
					Logger.error(e.getMessage());
				}
				getViewers = null;
			}
		}

		if (getSubscribers != null) {
			if (getSubscribers.isDone()) {
				try {
					twitchDataStore.setCurrentSubscribers(getSubscribers.get());
				} catch (InterruptedException | ExecutionException e) {
					Logger.error(e.getMessage());
				}
				getSubscribers = null;
			}
		}
	}

	private void doInfrequentUpdate() {
		timeSinceInfrequentUpdate = 0f;

		if (twitchDataStore.isTwitchEnabled()) {
			if (getViewers == null) {
				getViewers = backgroundTaskManager.postUntrackedCallable(new GetTwitchViewers(twitchDataStore));
			}
			if (getSubscribers == null) {
				getSubscribers = backgroundTaskManager.postUntrackedCallable(new GetTwitchSubscribers(twitchDataStore));
			}
		}

	}
}
