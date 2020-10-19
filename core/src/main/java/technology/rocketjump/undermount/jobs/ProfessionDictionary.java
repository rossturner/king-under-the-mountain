package technology.rocketjump.undermount.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class ProfessionDictionary {

	private Map<String, Profession> byName = new HashMap<>();

	public static Profession NULL_PROFESSION = new Profession();
	public static Profession CONTEXT_DEPENDENT_PROFESSION_REQUIRED = new Profession();
	static {
		NULL_PROFESSION.setName("Null profession");
		NULL_PROFESSION.setI18nKey("PROFESSION.VILLAGER");
		NULL_PROFESSION.setIcon("profession-none");
		CONTEXT_DEPENDENT_PROFESSION_REQUIRED.setName("Specific profession required");
	}

	@Inject
	public ProfessionDictionary() throws IOException {
		this(new File("assets/definitions/types/professions.json"));
	}

	public ProfessionDictionary(File professionsJsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<Profession> professions = objectMapper.readValue(FileUtils.readFileToString(professionsJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, Profession.class));

		for (Profession profession : professions) {
			byName.put(profession.getName(), profession);
		}
	}

	public Profession getByName(String name) {
		return byName.get(name);
	}

	public Collection<Profession> getAll() {
		return byName.values();
	}

	public Profession getDefault() {
		return byName.get("VILLAGER");
	}
}
