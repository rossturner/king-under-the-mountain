package technology.rocketjump.undermount.entities.tags;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class TagDictionary {

	private Map<String, Class<? extends Tag>> byTagName = new HashMap<>();

	@Inject
	public TagDictionary() throws ReflectiveOperationException {
		Reflections reflections = new Reflections("technology.rocketjump.undermount", new SubTypesScanner());
		Set<Class<? extends Tag>> tagClasses = reflections.getSubTypesOf(Tag.class);
		for (Class<? extends Tag> tagClass : tagClasses) {
			Tag instance = tagClass.getDeclaredConstructor().newInstance();
			byTagName.put(instance.getTagName(), tagClass);
		}
	}

	public Tag newInstanceByName(String tagName) {
		Class<? extends Tag> tagClass = byTagName.get(tagName);
		if (tagClass == null) {
			Logger.error("Could not find tag class by name: " + tagName);
			return null;
		} else {
			try {
				return tagClass.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				Logger.error(e, "Error while instantiating " + tagName + " tag");
				return null;
			}
		}
	}

}
