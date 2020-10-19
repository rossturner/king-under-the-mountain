package technology.rocketjump.undermount.ui.i18n;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;
import technology.rocketjump.undermount.persistence.UserPreferences;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.LANGUAGE;

@RunWith(MockitoJUnitRunner.class)
public class I18nRepoTest {

	private I18nRepo i18nRepo;
	@Mock
	private TextureAtlasRepository mockAtlasRepo;
	@Mock
	private TextureAtlas mockAtlas;
	@Mock
	private UserPreferences mockUserPreferences;

	@Before
	public void setUp() throws Exception {
		when(mockUserPreferences.getPreference(eq(LANGUAGE), any())).thenReturn("en-gb");
		this.i18nRepo = new I18nRepo(mockUserPreferences);

		when(mockAtlasRepo.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS)).thenReturn(mockAtlas);
		i18nRepo.init(mockAtlasRepo);
	}

	@Test
	public void getWord() throws Exception {
		I18nWord bakerWord = i18nRepo.getCurrentLanguage().getWord("PROFESSION.CHEF");
		assertThat(bakerWord.get(I18nWordClass.NOUN)).isEqualTo("Chef");
		assertThat(bakerWord.get(I18nWordClass.VERB)).isEqualTo("Cooking");
		assertThat(bakerWord.get(I18nWordClass.UNSPECIFIED)).isEqualTo("Chef");

		LanguageType nextLanguage = i18nRepo.getAllLanguages().get(1);
		assertThat(nextLanguage.getLabelEn()).isEqualTo("German");
		assertThat(nextLanguage.getLabel()).isEqualTo("Deutsch");
		assertThat(nextLanguage.getIcon()).isEqualTo("Germany");

		i18nRepo.setCurrentLanguage(nextLanguage);

		I18nWord languageWord = i18nRepo.getCurrentLanguage().getWord("LANGUAGE.LABEL");
		assertThat(languageWord.get(I18nWordClass.UNSPECIFIED)).isEqualTo("Sprache");
	}

}