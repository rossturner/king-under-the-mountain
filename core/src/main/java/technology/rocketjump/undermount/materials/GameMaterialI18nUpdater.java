package technology.rocketjump.undermount.materials;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.ui.i18n.I18nLanguageDictionary;
import technology.rocketjump.undermount.ui.i18n.I18nRepo;

@Singleton
public class GameMaterialI18nUpdater {

	private final I18nRepo i18nRepo;
	private final GameMaterialDictionary gameMaterialDictionary;

	@Inject
	public GameMaterialI18nUpdater(I18nRepo i18nRepo, GameMaterialDictionary gameMaterialDictionary) {
		this.i18nRepo = i18nRepo;
		this.gameMaterialDictionary = gameMaterialDictionary;
	}

	public void onLanguageUpdated() {
		I18nLanguageDictionary currentLanguage = i18nRepo.getCurrentLanguage();
		for (GameMaterialType materialType : GameMaterialType.values()) {
			materialType.setI18NValue(currentLanguage.getWord(materialType.getI18nKey()));
		}
		for (GameMaterial gameMaterial : gameMaterialDictionary.getAll()) {
			if (!gameMaterial.isDynamicallyCreated()) {
				if (gameMaterial.isUseMaterialTypeAsAdjective()) {
					gameMaterial.setI18nValue(gameMaterial.getMaterialType().getI18nValue());
				} else {
					gameMaterial.setI18nValue(currentLanguage.getWord(gameMaterial.getI18nKey()));
				}
			}
		}
	}
}
