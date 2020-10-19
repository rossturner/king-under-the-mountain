package technology.rocketjump.undermount.messaging;

/**
 * This enum is returned from asynchronous tasks when they need to display an error back to the user
 */
public enum ErrorType {

	WHILE_SAVING("GUI.DIALOG.ERROR_WHILE_SAVING"),
	WHILE_LOADING("GUI.DIALOG.ERROR_WHILE_LOADING"),
	INVALID_SAVE_FILE("GUI.DIALOG.ERROR_SAVE_INCOMPATIBLE"),
	LOCATION_COMPONENT_LOST("LOCATION_COMPONENT_LOST"),
	ROOM_NAME_ALREADY_EXISTS("GUI.DIALOG.ERROR_ROOM_NAME_ALREADY_EXISTS");

	public final String i18nKey;

	ErrorType(String i18nKey) {
		this.i18nKey = i18nKey;
	}
}
