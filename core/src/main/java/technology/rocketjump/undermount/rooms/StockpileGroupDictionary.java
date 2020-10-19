package technology.rocketjump.undermount.rooms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class StockpileGroupDictionary {

	private Map<String, StockpileGroup> byName = new HashMap<>();

	@Inject
	public StockpileGroupDictionary() throws IOException {
		this(new File("assets/definitions/stockpileGroups.json"));
	}

	private StockpileGroupDictionary(File jsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<StockpileGroup> stockpileGroups = objectMapper.readValue(FileUtils.readFileToString(jsonFile),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, StockpileGroup.class));

		for (StockpileGroup stockpileGroup : stockpileGroups) {
			byName.put(stockpileGroup.getName(), stockpileGroup);
		}
	}

	public StockpileGroup getByName(String stockpileGroupName) {
		return byName.get(stockpileGroupName);
	}

}
