package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.jobs.model.Profession;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;

@Singleton
public class ImageButtonFactory {

	private final TextureAtlas textureAtlas;
	private final NinePatch buttonNinePatch;

	private Map<String, ImageButton> byIconName = new HashMap<>();

	@Inject
	public ImageButtonFactory(TextureAtlasRepository textureAtlasRepository, ProfessionDictionary professionDictionary) {
		this.textureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		this.buttonNinePatch = textureAtlas.createPatch("button");

		for (Profession profession : professionDictionary.getAll()) {
			profession.setImageButton(create(profession.getIcon()));
		}
		NULL_PROFESSION.setImageButton(create(NULL_PROFESSION.getIcon()));
	}

	public ImageButton create(String iconName) {
		return create(iconName,false);
	}

	public ImageButton create(String iconName, boolean halfSize) {

		return byIconName.computeIfAbsent(iconName, (i) -> {
			Sprite iconSprite = this.textureAtlas.createSprite(iconName);
			if (iconSprite == null) {
				throw new RuntimeException("Could not find UI sprite with name " + iconName);
			}
			return new ImageButton(iconSprite, buttonNinePatch, halfSize);
		});
	}

}
