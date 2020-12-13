package technology.rocketjump.undermount.misc.twitch.tasks;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import technology.rocketjump.undermount.misc.twitch.TwitchDataStore;
import technology.rocketjump.undermount.misc.twitch.TwitchRequestHandler;
import technology.rocketjump.undermount.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.undermount.misc.twitch.model.TwitchViewer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class GetTwitchSubscribers implements Callable<List<TwitchViewer>> {

	private final TwitchRequestHandler twitchRequestHandler = new TwitchRequestHandler();
	private final TwitchDataStore twitchDataStore;

	public GetTwitchSubscribers(TwitchDataStore twitchDataStore) {
		this.twitchDataStore = twitchDataStore;
	}

	@Override
	public List<TwitchViewer> call() throws Exception {
		TwitchAccountInfo accountInfo = twitchDataStore.getAccountInfo();
		if (accountInfo == null) {
			throw new Exception("Account info is null");
		}

		Response response = twitchRequestHandler.get("https://api.twitch.tv/helix/subscriptions?broadcaster_id="+accountInfo.getUser_id(), twitchDataStore);

		try {
			if (response.isSuccessful()) {
				List<TwitchViewer> subscribers = new ArrayList<>();
				JSONObject responseJson = JSON.parseObject(response.body().string());

				JSONArray data = responseJson.getJSONArray("data");
				for (int cursor = 0; cursor < data.size(); cursor++) {
					JSONObject subscription = data.getJSONObject(cursor);
					String username = subscription.getString("user_name");
					if (!username.endsWith("bot")) {
						subscribers.add(new TwitchViewer(username));
					}
				}
				return subscribers;
			} else {
				throw new Exception("Received " + response.code() + " while calling " + this.getClass().getSimpleName());
			}
		} finally {
			IOUtils.closeQuietly(response);
		}
	}
}
