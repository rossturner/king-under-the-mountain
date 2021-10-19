package technology.rocketjump.undermount.mapping.tile.designation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.ui.GameInteractionMode;

import java.io.IOException;
import java.util.*;

@Singleton
public class DesignationDictionary {

	private Map<String, Designation> byName = new HashMap<>();

	@Inject
	public DesignationDictionary(TextureAtlasRepository textureAtlasRepository, JobTypeDictionary jobTypeDictionary) throws IOException {
		this(Gdx.files.internal("assets/definitions/designations.json"), textureAtlasRepository, jobTypeDictionary);
	}

	public DesignationDictionary(FileHandle designationsJson, TextureAtlasRepository textureAtlasRepository,
								 JobTypeDictionary jobTypeDictionary) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<Designation> allDesignations = objectMapper.readValue(designationsJson.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, Designation.class));

		TextureAtlas guiAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);

		for (Designation designation : allDesignations) {
			if (designation.getCreatesJobTypeName() != null) {
				JobType jobType = jobTypeDictionary.getByName(designation.getCreatesJobTypeName());
				if (jobType == null) {
					Logger.error("Could not find job type with name " + designation.getCreatesJobTypeName());
				} else {
					designation.setCreatesJobType(jobType);
				}
			}

			Sprite sprite = guiAtlas.createSprite(designation.getIconName());
			if (sprite == null) {
				throw new RuntimeException("No sprite found in GUI atlas by name: " + designation.getIconName());
			}
			designation.setIconSprite(sprite);
			byName.put(designation.getDesignationName(), designation);
		}
	}

	public Designation getByName(String designationName) {
		return byName.get(designationName);
	}

	public void init() {
		GameInteractionMode.init(this);
	}

	public Collection<Designation> getAll() {
		return byName.values();
	}
}
