package technology.rocketjump.undermount.rooms;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nUpdatable;

import java.io.IOException;

@Singleton
public class RoomTypeI18nProcessor implements I18nUpdatable {

	private final RoomTypeDictionary roomTypeDictionary;
	private final I18nTranslator translator;

	@Inject
	private RoomTypeI18nProcessor(RoomTypeDictionary roomTypeDictionary, I18nTranslator translator) throws IOException {
		this.roomTypeDictionary = roomTypeDictionary;
		this.translator = translator;
		onLanguageUpdated();
	}

	@Override
	public void onLanguageUpdated() {
		roomTypeDictionary.byTranslatedName.clear();
		for (RoomType roomType : roomTypeDictionary.getAll()) {
			roomType.setI18nValue(translator.getTranslatedString(roomType.getI18nKey()));
			roomTypeDictionary.byTranslatedName.put(roomType.getI18nValue().toString(), roomType);
		}
	}
}
