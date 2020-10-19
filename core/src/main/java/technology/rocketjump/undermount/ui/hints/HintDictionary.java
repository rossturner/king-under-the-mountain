package technology.rocketjump.undermount.ui.hints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.ui.hints.model.Hint;
import technology.rocketjump.undermount.ui.hints.model.HintTrigger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class HintDictionary {

	private final Map<String, Hint> byId = new HashMap<>();
	private final Map<HintTrigger.HintTriggerType, List<Hint>> byTriggerType = new HashMap<>();

	@Inject
	public HintDictionary() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		File hintsJsonFile = new File("assets/ui/hints.json");
		List<Hint> hints = objectMapper.readValue(FileUtils.readFileToString(hintsJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, Hint.class));

		for (Hint hint : hints) {
			byId.put(hint.getHintId(), hint);
			for (HintTrigger trigger : hint.getTriggers()) {
				if (trigger.getTriggerType() != null) {
					byTriggerType.computeIfAbsent(trigger.getTriggerType(), a -> new ArrayList<>()).add(hint);
				}
			}
		}
	}

	public Hint getById(String hintId) {
		return byId.get(hintId);
	}

	private static final List<Hint> EMPTY_LIST = new ArrayList<>();

	public List<Hint> getByTriggerType(HintTrigger.HintTriggerType triggerType) {
		return byTriggerType.getOrDefault(triggerType, EMPTY_LIST);
	}


}
