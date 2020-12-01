package technology.rocketjump.undermount.misc.twitch.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import technology.rocketjump.undermount.misc.twitch.TwitchDataStore;
import technology.rocketjump.undermount.misc.twitch.TwitchRequestHandler;
import technology.rocketjump.undermount.misc.twitch.model.TwitchAccountInfo;

import java.util.concurrent.Callable;

public class GetTwitcAccountInfo implements Callable<TwitchAccountInfo> {

	private final TwitchRequestHandler twitchRequestHandler;
	private final TwitchDataStore twitchDataStore;

	public GetTwitcAccountInfo(TwitchRequestHandler twitchRequestHandler, TwitchDataStore twitchDataStore) {
		this.twitchRequestHandler = twitchRequestHandler;
		this.twitchDataStore = twitchDataStore;
	}

	@Override
	public TwitchAccountInfo call() throws Exception {
		Response response = twitchRequestHandler.get("https://id.twitch.tv/oauth2/validate", twitchDataStore);

		if (response.isSuccessful()) {
			return new ObjectMapper().readValue(response.body().string(), TwitchAccountInfo.class);
		} else {
			throw new Exception("Received " + response.code() + " while calling " + this.getClass().getSimpleName());
		}
	}

}
