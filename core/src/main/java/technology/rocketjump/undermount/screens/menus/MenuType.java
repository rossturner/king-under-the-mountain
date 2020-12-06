package technology.rocketjump.undermount.screens.menus;

public enum MenuType {

	PRIVACY_OPT_IN_MENU(PrivacyOptInMenu.class),
	TOP_LEVEL_MENU(TopLevelMenu.class),
	EMBARK_MENU(EmbarkMenu.class),
	OPTIONS_MENU(OptionsMenu.class),
	MODS_MENU(ModsMenu.class);

	private final Class<? extends Menu> relatedMenuClass;

	MenuType(Class<? extends Menu> relatedMenuClass) {
		this.relatedMenuClass = relatedMenuClass;
	}

	public static MenuType byInstance(Menu menuInstance) {
		if (menuInstance == null) {
			return null;
		}
		for (MenuType menuValue : MenuType.values()) {
			if (menuValue.relatedMenuClass.equals(menuInstance.getClass())) {
				return menuValue;
			}
		}
		throw new RuntimeException("Unrecognised menu type: " + menuInstance.getClass().getSimpleName());
	}

}
