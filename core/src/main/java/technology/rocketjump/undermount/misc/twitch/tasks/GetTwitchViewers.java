package technology.rocketjump.undermount.misc.twitch.tasks;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.misc.twitch.TwitchDataStore;
import technology.rocketjump.undermount.misc.twitch.TwitchViewer;
import technology.rocketjump.undermount.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class GetTwitchViewers implements Callable<List<TwitchViewer>> {

	private final TwitchDataStore twitchDataStore;

	public GetTwitchViewers(TwitchDataStore twitchDataStore) {
		this.twitchDataStore = twitchDataStore;
	}

	@Override
	public List<TwitchViewer> call() throws Exception {
		TwitchAccountInfo accountInfo = twitchDataStore.getAccountInfo();
		if (accountInfo == null) {
			throw new Exception("Account info is null");
		}

		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder()
//				.url("https://tmi.twitch.tv/group/user/"+accountInfo.getLogin()+"/chatters")
				.url("https://tmi.twitch.tv/group/user/"+"bruceoakman"+"/chatters")
				.get()
				.build();

		Response response = client.newCall(request).execute();

		if (GlobalSettings.DEV_MODE) {
			Logger.debug("Request to " + request.url().toString() + " returned " + response.code());
		}

		if (response.isSuccessful()) {
			List<TwitchViewer> viewers = new ArrayList<>();
			JSONObject responseJson = JSON.parseObject(response.body().string());
			JSONObject chatters = responseJson.getJSONObject("chatters");

			for (String chatterType : Arrays.asList("vips", "moderators", "viewers")) {
				for (Object nameObj : chatters.getJSONArray(chatterType)) {
					String username = nameObj.toString();
					if (!username.endsWith("bot")) {
						viewers.add(new TwitchViewer(username));
					}
				}
			}

			return viewers;
		} else {
			throw new Exception("Received " + response.code() + " while calling " + this.getClass().getSimpleName());
		}
	}

	private void getViewers(JSONArray nameArray, List<TwitchViewer> viewers) {
		for (Object nameObj : nameArray) {
		}
	}
}
