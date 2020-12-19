package technology.rocketjump.undermount.misc.twitch;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.misc.twitch.model.TwitchToken;
import technology.rocketjump.undermount.persistence.UserPreferences;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.TWITCH_TOKEN;

@Singleton
public class TwitchMessageHandler implements Telegraph {

	private final TwitchTaskRunner twitchTaskRunner;
	private final UserPreferences userPreferences;

	@Inject
	public TwitchMessageHandler(MessageDispatcher messageDispatcher, TwitchTaskRunner twitchTaskRunner, UserPreferences userPreferences) {
		this.twitchTaskRunner = twitchTaskRunner;
		this.userPreferences = userPreferences;

		messageDispatcher.addListener(this, MessageType.TWITCH_AUTH_CODE_SUPPLIED);
		messageDispatcher.addListener(this, MessageType.TWITCH_TOKEN_UPDATED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.TWITCH_AUTH_CODE_SUPPLIED: {
				String code = (String) msg.extraInfo;
				twitchTaskRunner.authenticateWithCode(code);
				return true;
			}
			case MessageType.TWITCH_TOKEN_UPDATED: {
				TwitchToken token = (TwitchToken) msg.extraInfo;
				try {
					if (token == null) {
						userPreferences.removePreference(TWITCH_TOKEN);
					} else {
						userPreferences.setPreference(TWITCH_TOKEN, new ObjectMapper().writeValueAsString(token));
					}
				} catch (JsonProcessingException e) {
					Logger.error(e);
				}


				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}
}
