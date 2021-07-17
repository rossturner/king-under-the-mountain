package technology.rocketjump.undermount.materials;

import com.badlogic.gdx.graphics.Color;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.rendering.utils.ColorMixer;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Singleton
public class DynamicMaterialFactory implements I18nUpdatable, GameContextAware {

	private final GameMaterialDictionary gameMaterialDictionary;
	private final I18nTranslator i18nTranslator;
	private GameContext gameContext;

	@Inject
	public DynamicMaterialFactory(GameMaterialDictionary gameMaterialDictionary, I18nTranslator i18nTranslator) {
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.i18nTranslator = i18nTranslator;
	}

	public GameMaterial generate(List<GameMaterial> inputMaterials, GameMaterialType materialType, boolean isAlcoholic, boolean isEdible, String i18nDescriptionKey) {
		if (inputMaterials == null || inputMaterials.isEmpty()) {
			return null;
		}
		Collections.sort(inputMaterials);
		String combinedId = combineIds(inputMaterials, materialType, isAlcoholic, isEdible);
		GameMaterial existing = gameContext.getDynamicallyCreatedMaterialsByCombinedId().get(combinedId);
		if (existing != null) {
			return existing;
		} else {
			Color combinedColor = null;
			Set<GameMaterial> materialSet = new TreeSet<>();
			boolean isPoisonous = false;
			for (GameMaterial inputMaterial : inputMaterials) {
				if (combinedColor == null) {
					combinedColor = inputMaterial.getColor();
				} else {
					combinedColor = ColorMixer.average(combinedColor, inputMaterial.getColor());
				}
				materialSet.add(inputMaterial);
				if (inputMaterial.isPoisonous()) {
					isPoisonous = true;
				}
			}
			GameMaterial created = new GameMaterial(combinedId,
					"Combined " + StringUtils.join(materialSet, " "), SequentialIdGenerator.nextId(),
					materialType, combinedColor, isAlcoholic, false, isPoisonous, isEdible, false, materialSet);
			created.setI18nKey(i18nDescriptionKey);
			updateI18nValue(created);

			gameContext.getDynamicallyCreatedMaterialsByCombinedId().put(combinedId, created);
			if (!gameMaterialDictionary.contains(created)) {
				gameMaterialDictionary.add(created);
			}
			return created;
		}
	}

	@Override
	public void onLanguageUpdated() {
		if (gameContext != null) {
			for (GameMaterial gameMaterial : gameContext.getDynamicallyCreatedMaterialsByCombinedId().values()) {
				updateI18nValue(gameMaterial);
			}
		}
	}

	private void updateI18nValue(GameMaterial gameMaterial) {
		if (gameMaterial.isUseMaterialTypeAsAdjective()) {
			gameMaterial.setI18nValue(gameMaterial.getMaterialType().getI18nValue());
		} else {
			gameMaterial.setI18nValue(i18nTranslator.getDynamicMaterialDescription(gameMaterial));
		}
	}

	private String combineIds(List<GameMaterial> inputMaterials, GameMaterialType materialType, boolean isAlcoholic, boolean isEdible) {
		StringBuilder builder = new StringBuilder();
		builder.append(materialType.name());
		for (GameMaterial inputMaterial : inputMaterials) {
			builder.append("_").append(inputMaterial.getMaterialId());
		}
		if (isAlcoholic) {
			builder.append("_ALC");
		}
		if (isEdible) {
			builder.append("_EDI");
		}
		return builder.toString();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		for (GameMaterial dynamicMaterial : gameContext.getDynamicallyCreatedMaterialsByCombinedId().values()) {
			gameMaterialDictionary.add(dynamicMaterial);
		}
	}

	@Override
	public void clearContextRelatedState() {
		gameMaterialDictionary.clearDynamicMaterials();
	}
}
