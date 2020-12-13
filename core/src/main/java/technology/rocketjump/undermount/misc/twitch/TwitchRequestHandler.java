package technology.rocketjump.undermount.misc.twitch;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.misc.twitch.model.TwitchToken;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;

import java.io.IOException;

import static technology.rocketjump.undermount.misc.twitch.tasks.GetTwitchAuthToken.emptyRequestBody;

public class TwitchRequestHandler {

	public static final String CLIENT_ID = "6gk8asspwcrt787lxge71kc418a3ng";

	private final OkHttpClient client = new OkHttpClient();

	public Response get(String url, TwitchDataStore twitchDataStore) throws Exception {
		return get(url, twitchDataStore, false);
	}

	private Response get(String url, TwitchDataStore twitchDataStore, boolean refreshAttempted) throws Exception {
		TwitchToken currentToken = twitchDataStore.getCurrentToken();
		if (currentToken == null) {
			throw new Exception("No token available");
		}

		Request request = new Request.Builder()
				.url(url)
				.header("Client-ID", CLIENT_ID)
				.header("Authorization", "Bearer " + currentToken.getAccess_token())
				.get()
				.build();

		Response response = client.newCall(request).execute();
		try {
			if (GlobalSettings.DEV_MODE) {
				Logger.debug("Request to " + request.url().toString() + " returned " + response.code());
			}

			if (response.code() == 401 && !refreshAttempted) {
				attemptRefreshToken(twitchDataStore);
				return get(url, twitchDataStore, true);
			} else {
				return response;
			}
		} finally {
			IOUtils.closeQuietly(response);
		}
	}

	private void attemptRefreshToken(TwitchDataStore twitchDataStore) throws IOException {
		Request request = new Request.Builder()
				.url("https://undermount-api.herokuapp.com/api/twitch/oauth/refresh?token=" + twitchDataStore.getCurrentToken().getRefresh_token())
				.post(emptyRequestBody)
				.build();

		Response response = client.newCall(request).execute();

		try {
			if (response.isSuccessful()) {
				TwitchToken newToken = new ObjectMapper().readValue(response.body().string(), TwitchToken.class);
				twitchDataStore.setCurrentToken(newToken);
			}
		} finally {
			IOUtils.closeQuietly(response);
		}
	}
}
