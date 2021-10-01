package technology.rocketjump.undermount;

import com.google.inject.AbstractModule;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.persistence.UserFileManager;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.ui.i18n.I18nRepo;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.io.IOException;

public class TestModule extends AbstractModule {

	@Override
	protected void configure() {
		try {
			bind(I18nTranslator.class).toInstance(stubbedI18nTranslater());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private I18nTranslator stubbedI18nTranslater() throws IOException {
		return new I18nTranslator(
				new I18nRepo(new UserPreferences(new UserFileManager())),
				new ProfessionDictionary(),
				null
		);
	}

}
