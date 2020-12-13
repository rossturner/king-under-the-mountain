package technology.rocketjump.undermount.messaging;

/**
 * This enum is returned from asynchronous tasks when they need to display an error back to the user
 */
public enum InfoType {

	LANGUAGE_TRANSLATION_INCOMPLETE("GUI.DIALOG.LANGUAGE_TRANSLATION_INCOMPLETE"),
	MOD_CHANGES_OUTSTANDING("GUI.DIALOG.MOD_CHANGES_OUTSTANDING"),
	MOD_INCOMPATIBLE("GUI.DIALOG.MOD_INCOMPATIBLE"),
	SETTLEMENT_NAME_NOT_SPECIFIED("GUI.DIALOG.SETTLEMENT_NAME_NOT_SPECIFIED"),
	RESTART_REQUIRED("GUI.DIALOG.RESTART_REQUIRED");

	public final String i18nKey;

	InfoType(String i18nKey) {
		this.i18nKey = i18nKey;
	}
}
