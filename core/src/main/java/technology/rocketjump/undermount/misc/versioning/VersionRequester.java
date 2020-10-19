package technology.rocketjump.undermount.misc.versioning;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.messaging.MessageType;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class VersionRequester implements Runnable {

	private final MessageDispatcher messageDispatcher;

	public VersionRequester(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public void run() {
		String channelName = guessChannelName();
		if (channelName == null) {
			return;
		}

		OkHttpClient client = trustAllSslClient();

		Request request = new Request.Builder()
				.url("https://itch.io/api/1/x/wharf/latest?target=rocketjumptechnology/king-under-the-mountain&channel_name="+channelName)
				.get()
				.build();
		try {
			Response response = client.newCall(request).execute();
			if (response.isSuccessful()) {
				JSONObject responseJson = JSON.parseObject(response.body().string());
				Version latestVersion = new Version(responseJson.getString("latest"));
				messageDispatcher.dispatchMessage(MessageType.REMOTE_VERSION_FOUND, latestVersion);
			}
		} catch (Exception e) {
			Logger.error(e);
		}
	}

	private String guessChannelName() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("windows")) {
			return "win";
		} else if (osName.contains("osx") || osName.contains("mac")) {
			return "osx";
		} else if (osName.contains("nix") || osName.contains("nux")) {
			return "linux";
		} else {
			Logger.warn("Could not guess OS from " + osName);
			return null;
		}
	}

	// Only scraping itch.io for version number over HTTPS so somewhat safe to ignore SSL
	private OkHttpClient trustAllSslClient() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)trustAllCerts[0]);
		builder.hostnameVerifier((hostname, session) -> true);
		return builder.build();
	}

	private static final TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[]{};
				}
			}
	};

	private static final SSLContext trustAllSslContext;
	static {
		try {
			trustAllSslContext = SSLContext.getInstance("SSL");
			trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}
	private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();
}
