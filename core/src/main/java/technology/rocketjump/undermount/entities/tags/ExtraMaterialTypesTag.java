package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ExtraMaterialTypesTag extends Tag {

	@Override
	public String getTagName() {
		return "EXTRA_MATERIAL_TYPES";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		for (String arg : args) {
			if (!EnumUtils.isValidEnum(GameMaterialType.class, arg)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		Random random = new RandomXS128(entity.getId());
		switch (entity.getType()) {
			case FURNITURE: {
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				for (String arg : args) {
					GameMaterialType materialType = GameMaterialType.valueOf(arg);
					List<GameMaterial> materials = tagProcessingUtils.materialDictionary.getByType(materialType).stream()
							.filter(GameMaterial::isUseInRandomGeneration)
							.collect(Collectors.toList());

					GameMaterial gameMaterial = materials.get(random.nextInt(materials.size()));
					attributes.setMaterial(gameMaterial);
				}
				break;
			}
			default:
				Logger.error("Not yet implemented: " + getTagName() + " for " + entity.getType());
		}
	}

}
