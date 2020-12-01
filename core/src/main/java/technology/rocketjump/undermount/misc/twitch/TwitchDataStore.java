package technology.rocketjump.undermount.misc.twitch;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.undermount.misc.twitch.model.TwitchToken;
import technology.rocketjump.undermount.persistence.UserPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is used to store data scraped from Twitch, to be used in-game
 */
@Singleton
public class TwitchDataStore {

	private AtomicReference<TwitchToken> currentToken = new AtomicReference<>();
	private TwitchAccountInfo accountInfo;

	private List<TwitchViewer> currentSubscribers = new ArrayList<>();
	private List<TwitchViewer> currentViewers = new ArrayList<>();

	private final MessageDispatcher messageDispatcher;
	private final UserPreferences userPreferences;

	@Inject
	public TwitchDataStore(MessageDispatcher messageDispatcher, UserPreferences userPreferences) throws IOException {
		this.messageDispatcher = messageDispatcher;
		this.userPreferences = userPreferences;

		String tokenAsString = userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_TOKEN, null);
		if (tokenAsString != null) {
			currentToken.set(new ObjectMapper().readValue(tokenAsString, TwitchToken.class));
		}
	}

	public List<TwitchViewer> getPrioritisedViewers() {
		List<TwitchViewer> result = new ArrayList<>();
		boolean subscribersPrioritised = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_PRIORITISE_SUBSCRIBERS, "false"));

		List<TwitchViewer> liveSubscribers = new ArrayList<>();
		List<TwitchViewer> liveNonSubscribers = new ArrayList<>();

		for (TwitchViewer viewer : currentViewers) {
			if (currentSubscribers.contains(viewer)) {
				liveSubscribers.add(viewer);
			} else {
				liveNonSubscribers.add(viewer);
			}
		}

		if (subscribersPrioritised) {
			Collections.shuffle(liveSubscribers);
			Collections.shuffle(liveNonSubscribers);
			result.addAll(liveSubscribers);
			result.addAll(liveNonSubscribers);
		} else {
			result.addAll(liveSubscribers);
			result.addAll(liveNonSubscribers);
			Collections.shuffle(result);
		}

		return result;
	}

	public boolean isTwitchEnabled() {
		return Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_INTEGRATION_ENABLED, "false"));
	}

	public List<TwitchViewer> getCurrentViewers() {
		return null;
	}

	public TwitchToken getCurrentToken() {
		return currentToken.get();
	}

	public void setCurrentToken(TwitchToken currentToken) {
		this.currentToken.set(currentToken);
		messageDispatcher.dispatchMessage(MessageType.TWITCH_TOKEN_UPDATED, currentToken);
	}

	public TwitchAccountInfo getAccountInfo() {
		return accountInfo;
	}

	public void setAccountInfo(TwitchAccountInfo accountInfo) {
		this.accountInfo = accountInfo;
		messageDispatcher.dispatchMessage(MessageType.TWITCH_ACCOUNT_INFO_UPDATED, accountInfo);
	}

	public List<TwitchViewer> getCurrentSubscribers() {
		return currentSubscribers;
	}

	public void setCurrentSubscribers(List<TwitchViewer> currentSubscribers) {
		this.currentSubscribers = currentSubscribers;
	}

	public void setCurrentViewers(List<TwitchViewer> currentViewers) {
		this.currentViewers = currentViewers;
	}
}
