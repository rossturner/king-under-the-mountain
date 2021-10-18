package technology.rocketjump.undermount.materials.model;

import technology.rocketjump.undermount.ui.i18n.I18nWord;

public enum GameMaterialType {

	STONE, METAL, WOOD, ORE, GEM,
	CLOTH, ROPE,
	EARTH, SEED, VEGETABLE, FOODSTUFF, MEAT, BONE,
	LIQUID, VITRIOL,
	OTHER;

	private I18nWord i18NValue = new I18nWord(getI18nKey());

	public String getI18nKey() {
		return "MATERIAL_TYPE."+name();
	}

	public I18nWord getI18nValue() {
		return i18NValue;
	}

	public void setI18NValue(I18nWord i18NValue) {
		this.i18NValue = i18NValue;
	}

	@Override
	public String toString() {
		return i18NValue.toString();
	}
}
