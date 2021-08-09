package technology.rocketjump.undermount.entities.factories;

import com.badlogic.gdx.graphics.Color;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.List;
import java.util.Random;

@Singleton
public class AccessoryColorFactory {

	private final GameMaterialDictionary gameMaterialDictionary;

	@Inject
	public AccessoryColorFactory(GameMaterialDictionary gameMaterialDictionary) {
		this.gameMaterialDictionary = gameMaterialDictionary;
	}

	public Color randomAccessoryColor(Random random) {
		List<GameMaterial> metalMaterials = gameMaterialDictionary.getByType(GameMaterialType.METAL);
		return metalMaterials.get(random.nextInt(metalMaterials.size())).getColor();
	}
}
