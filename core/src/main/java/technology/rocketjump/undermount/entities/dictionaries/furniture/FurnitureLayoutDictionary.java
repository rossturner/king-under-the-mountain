package technology.rocketjump.undermount.entities.dictionaries.furniture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class FurnitureLayoutDictionary {

	private Map<String, FurnitureLayout> byName = new HashMap<>();

	public static FurnitureLayout NULL_LAYOUT = new FurnitureLayout();
	static {
		NULL_LAYOUT.setUniqueName("Null category");
	}

	@Inject
	public FurnitureLayoutDictionary() throws IOException {
		this(new File("assets/definitions/types/furnitureLayouts.json"));
	}

	public FurnitureLayoutDictionary(File jsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<FurnitureLayout> layouts = objectMapper.readValue(FileUtils.readFileToString(jsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, FurnitureLayout.class));

		for (FurnitureLayout layout : layouts) {
			byName.put(layout.getUniqueName(), layout);
		}

		for (FurnitureLayout layout : layouts) {
			if (layout.getRotatesToName() != null) {
				layout.setRotatesTo(byName.get(layout.getRotatesToName()));
			}
		}

		byName.put(NULL_LAYOUT.getUniqueName(), NULL_LAYOUT);
	}

	public FurnitureLayout getByName(String name) {
		return byName.get(name);
	}

	public Collection<FurnitureLayout> getAll() {
		return byName.values();
	}
}
