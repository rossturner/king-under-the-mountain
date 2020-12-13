package technology.rocketjump.undermount.misc.twitch.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import technology.rocketjump.undermount.misc.twitch.model.TwitchToken;

import java.util.concurrent.Callable;

public class GetTwitchAuthToken implements Callable<TwitchToken> {

	private final String authCode;
	public static final RequestBody emptyRequestBody = RequestBody.create(null, new byte[0]);

	public GetTwitchAuthToken(String authCode) {
		this.authCode = authCode;
	}

	@Override
	public TwitchToken call() throws Exception {
		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder()
				.url("https://undermount-api.herokuapp.com/api/twitch/oauth/token?code=" + authCode)
				.post(emptyRequestBody)
				.build();

		Response response = client.newCall(request).execute();
		try {
			if (response.isSuccessful()) {
				return new ObjectMapper().readValue(response.body().string(), TwitchToken.class);
			} else {
				throw new Exception("Received status code " + response.code() + " from " + getClass().getSimpleName());
			}
		} finally {
			IOUtils.closeQuietly(response);
		}

	}

}
