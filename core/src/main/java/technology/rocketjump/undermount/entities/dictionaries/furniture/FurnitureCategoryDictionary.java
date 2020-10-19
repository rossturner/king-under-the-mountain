package technology.rocketjump.undermount.entities.dictionaries.furniture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureCategory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class FurnitureCategoryDictionary {

	private Map<String, FurnitureCategory> byName = new HashMap<>();

	public static FurnitureCategory NULL_CATEGORY = new FurnitureCategory();
	static {
		NULL_CATEGORY.setName("Null category");
	}

	@Inject
	public FurnitureCategoryDictionary() throws IOException {
		this(new File("assets/definitions/types/furnitureCategories.json"));
	}

	public FurnitureCategoryDictionary(File categoriesJsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<FurnitureCategory> categories = objectMapper.readValue(FileUtils.readFileToString(categoriesJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, FurnitureCategory.class));

		for (FurnitureCategory furnitureCategory : categories) {
			byName.put(furnitureCategory.getName(), furnitureCategory);
		}
		byName.put(NULL_CATEGORY.getName(), NULL_CATEGORY);
	}

	public FurnitureCategory getByName(String name) {
		return byName.get(name);
	}

	public Collection<FurnitureCategory> getAll() {
		return byName.values();
	}
}
