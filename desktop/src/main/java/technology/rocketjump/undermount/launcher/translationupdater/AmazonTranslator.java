package technology.rocketjump.undermount.launcher.translationupdater;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClientBuilder;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;
import org.pmw.tinylog.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmazonTranslator {

	private static final Map<String, String> SPECIAL_CASE_LANGUAGES = new HashMap<>();
	private final AmazonTranslate client;

	static {
		SPECIAL_CASE_LANGUAGES.put("zh_TW", "zh-TW");
		SPECIAL_CASE_LANGUAGES.put("zh-cn", "zh");
	}

	private List<String> unsupportedLanguages = Arrays.asList("pt-br");

	public AmazonTranslator() {
		AmazonTranslateClientBuilder clientBuilder = AmazonTranslateClientBuilder.standard();
		clientBuilder.setRegion(Regions.DEFAULT_REGION.getName());
		client = clientBuilder.build();
	}

	public String getTranslation(String sourceString, String sourceLangCode, String targetLangCode) {
		if (unsupportedLanguages.contains(targetLangCode.toLowerCase())) {
			return "";
		}
		TranslateTextRequest request = new TranslateTextRequest();
		request.setSourceLanguageCode(sourceLangCode);
		request.setTargetLanguageCode(getLanguageCode(targetLangCode));
		request.setText(sourceString);

		TranslateTextResult result = client.translateText(request);

		Logger.info("Translated '" + sourceString + "' to '" + result.getTranslatedText() + "'");

		return result.getTranslatedText();
	}

	public String getLanguageCode(final String input) {
		if (SPECIAL_CASE_LANGUAGES.containsKey(input)) {
			return SPECIAL_CASE_LANGUAGES.get(input);
		}

		if (input.contains("_")) {
			return input.substring(0, input.indexOf("_"));
		}

		return input;
	}
}
