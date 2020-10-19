package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.ErrorType;
import technology.rocketjump.undermount.messaging.InfoType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.settlement.notifications.Notification;
import technology.rocketjump.undermount.ui.i18n.I18nString;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.tooltips.I18nTextElement;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class GameDialogDictionary implements I18nUpdatable {

	private final I18nTranslator translator;
	private final Skin uiSkin;
	private final MessageDispatcher messageDispatcher;

	private final Map<ErrorType, ModalDialog> byErrorType = new EnumMap<>(ErrorType.class);
	private final Map<InfoType, ModalDialog> byInfoType = new EnumMap<>(InfoType.class);

	@Inject
	public GameDialogDictionary(I18nTranslator translator, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher) {
		this.translator = translator;
		this.uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;

		createDialogs();
	}

	public ModalDialog getErrorDialog(ErrorType errorType) {
		return byErrorType.get(errorType);
	}

	public ModalDialog getInfoDialog(InfoType infoType) {
		return byInfoType.get(infoType);
	}

	@Override
	public void onLanguageUpdated() {
		byErrorType.clear();
		createDialogs();
	}

	private void createDialogs() {
		for (ErrorType errorType : ErrorType.values()) {
			ModalDialog dialog = create(errorType);
			byErrorType.put(errorType, dialog);
		}


		for (InfoType infoType : InfoType.values()) {
			ModalDialog dialog = create(infoType);
			byInfoType.put(infoType, dialog);
		}

	}

	private ModalDialog create(ErrorType errorType) {
		I18nText title = translator.getTranslatedString("GUI.DIALOG.ERROR_TITLE");
		I18nText descriptionText = translator.getTranslatedString(errorType.i18nKey).breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		I18nText buttonText = translator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

		return new ModalDialog(title, descriptionText, buttonText, uiSkin, messageDispatcher);
	}

	private ModalDialog create(InfoType infoType) {
		I18nText title = translator.getTranslatedString("GUI.DIALOG.INFO_TITLE");
		I18nText descriptionText = translator.getTranslatedString(infoType.i18nKey).breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		I18nText buttonText = translator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

		return new ModalDialog(title, descriptionText, buttonText, uiSkin, messageDispatcher);
	}

	public NotificationDialog create(Notification notification) {
		I18nText title = translator.getTranslatedString(notification.getType().getI18nTitleKey());

		Map<String, I18nString> replacements = new HashMap<>();
		for (Map.Entry<String, I18nText> replacement : notification.getTextReplacements()) {
			replacements.put(replacement.getKey(), replacement.getValue());
		}
		I18nText descriptionText = translator.getTranslatedWordWithReplacements(notification.getType().getI18nDescriptionKey(), replacements)
				.breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());

		I18nText dismissText = translator.getTranslatedString("GUI.DIALOG.DISMISS");

		NotificationDialog notificationDialog = new NotificationDialog(title, uiSkin, messageDispatcher);

		if (notification.getType().getImageFilename() != null) {
			Texture texture = new Texture("assets/ui/notifications/"+notification.getType().getImageFilename());
			notificationDialog.addTexture(texture);
		}

		notificationDialog.withText(descriptionText);

//		notificationDialog.getContentTable().add(new Label(descriptionText, uiSkin));

		if (notification.getWorldPosition() != null) {
			I18nText jumpToText = translator.getTranslatedString("GUI.DIALOG.JUMP_TO");
			notificationDialog.withButton(jumpToText, () -> {
				messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, notification.getWorldPosition());
			});
		}

		notificationDialog.withButton(dismissText);

		return notificationDialog;
	}

	// Might want to extract out a more generic "show a dialog with some replaced items" method
	public ModalDialog createModsMissingDialog(List<String> missingModNames) {
		I18nText title = translator.getTranslatedString("GUI.DIALOG.INFO_TITLE");
		I18nText descriptionText = translator.getTranslatedString("MODS.MISSING_MODS_DIALOG_TEXT").breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		descriptionText.getElements().add(I18nTextElement.lineBreak);
		descriptionText.getElements().add(I18nTextElement.lineBreak);
		for (String missingModName : missingModNames) {
			descriptionText.getElements().add(new I18nTextElement("- " + missingModName, null));
			descriptionText.getElements().add(I18nTextElement.lineBreak);
		}
		I18nText buttonText = translator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

		return new ModalDialog(title, descriptionText, buttonText, uiSkin, messageDispatcher);
	}

	public ModalDialog createModsMissingSaveExceptionDialog(List<String> missingModNames) {
		I18nText title = translator.getTranslatedString("GUI.DIALOG.ERROR_TITLE");
		I18nText descriptionText = translator.getTranslatedString("MODS.MISSING_MODS_SAVE_EXCEPTION_DIALOG_TEXT").breakAfterLength(translator.getCurrentLanguageType().getBreakAfterLineLength());
		descriptionText.getElements().add(I18nTextElement.lineBreak);
		descriptionText.getElements().add(I18nTextElement.lineBreak);
		for (String missingModName : missingModNames) {
			descriptionText.getElements().add(new I18nTextElement("- " + missingModName, null));
			descriptionText.getElements().add(I18nTextElement.lineBreak);
		}
		I18nText buttonText = translator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

		return new ModalDialog(title, descriptionText, buttonText, uiSkin, messageDispatcher);
	}

	static String[] splitLines(String translatedString) {
		if (translatedString.length() < 10) {
			return new String[] {translatedString};
		}
		int midpointCursor = translatedString.length() / 2 + 1;
		for (int cursor = midpointCursor; cursor > 0; cursor--) {
			if (translatedString.charAt(cursor) == ' ') {
				return new String[] {translatedString.substring(0, cursor) , translatedString.substring(cursor + 1)};
			}

		}
		return new String[] {};
	}
}
