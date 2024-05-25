package technology.rocketjump.undermount.materials;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.undermount.entities.EntityStore;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.guice.UndermountGuiceModule;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.ui.i18n.I18nRepo;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.LANGUAGE;

@RunWith(MockitoJUnitRunner.class)
public class DynamicMaterialFactoryTest {

	private GameMaterialDictionary gameMaterialDictionary;

	private DynamicMaterialFactory dynamicMaterialFactory;

	private I18nTranslator i18nTranslator;
	@Mock
	private ProfessionDictionary mockProfessionDictionary;
	@Mock
	private EntityStore mockEntityStore;

	private GameContext gameContext;
	@Mock
	private UserPreferences mockUserPreferences;

	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(new UndermountGuiceModule());
		gameMaterialDictionary = injector.getInstance(GameMaterialDictionary.class);
		injector.getInstance(GameMaterialI18nUpdater.class).onLanguageUpdated();
		when(mockUserPreferences.getPreference(eq(LANGUAGE), any())).thenReturn("en-gb");

		I18nRepo i18nRepo = new I18nRepo(mockUserPreferences);
		i18nTranslator = new I18nTranslator(i18nRepo, mockProfessionDictionary, mockEntityStore);

		dynamicMaterialFactory = new DynamicMaterialFactory(gameMaterialDictionary, i18nTranslator);

		gameContext = new GameContext();
		dynamicMaterialFactory.onContextChange(gameContext);
	}

	@Test
	public void generate_withSameInputs_doesNotCreateNewMaterial() throws Exception {
		GameMaterial one = dynamicMaterialFactory.generate(materials("Tomato", "Tomato", "Potato"), GameMaterialType.LIQUID, false, false, "I18N_KEY");
		GameMaterial two = dynamicMaterialFactory.generate(materials("Tomato", "Tomato", "Potato"), GameMaterialType.LIQUID, false, false, "I18N_KEY");

		assertThat(one).isEqualTo(two);
	}

	@Test
	public void generate_withDifferentInputs_createsNewMaterial() throws Exception {
		GameMaterial one = dynamicMaterialFactory.generate(materials("Tomato", "Tomato", "Potato"), GameMaterialType.LIQUID, true, false, "I18N_KEY");
		GameMaterial two = dynamicMaterialFactory.generate(materials("Tomato", "Tomato", "Potato"), GameMaterialType.LIQUID, false, false, "I18N_KEY");

		assertThat(one).isNotEqualTo(two);

		GameMaterial three = dynamicMaterialFactory.generate(materials("Wheat", "Tomato", "Potato"), GameMaterialType.LIQUID, false, false, "I18N_KEY");

		assertThat(two).isNotEqualTo(three);
	}

	@Test
	public void getDynamicMaterialDescription_forSingleIngredientSoup() {
		GameMaterial material = dynamicMaterialFactory.generate(materials("Tomato", "Tomato", "Tomato"), GameMaterialType.LIQUID,
				true, false, "COOKING.SOUP.DESCRIPTION");

		assertThat(material.getI18nValue().toString()).isEqualTo("Tomato soup");
	}

	@Test
	public void getDynamicMaterialDescription_forSingleIngredientOnceSoup() {
		GameMaterial material = dynamicMaterialFactory.generate(materials("Tomato"), GameMaterialType.LIQUID,
				true, false, "COOKING.SOUP.DESCRIPTION");

		assertThat(material.getI18nValue().toString()).isEqualTo("Tomato soup");
	}

	@Test
	public void getDynamicMaterialDescription_forDualIngredientSoup() {
		GameMaterial material = dynamicMaterialFactory.generate(materials("Tomato", "Carrot", "Carrot"), GameMaterialType.LIQUID,
				true, false, "COOKING.SOUP.DESCRIPTION");

		assertThat(material.getI18nValue().toString()).isEqualTo("Carrot and tomato soup");
	}

	@Test
	public void getDynamicMaterialDescription_forMultiIngredientSoup() {
		GameMaterial material = dynamicMaterialFactory.generate(materials("Tomato", "Carrot", "Potato"), GameMaterialType.LIQUID,
				true, false, "COOKING.SOUP.DESCRIPTION");

		assertThat(material.getI18nValue().toString()).isEqualTo("Carrot, potato and tomato soup");
	}

	public List<GameMaterial> materials(String... materialNames) {
		List<GameMaterial> inputs = new ArrayList<>();
		for (String materialName : materialNames) {
			inputs.add(gameMaterialDictionary.getByName(materialName));
		}
		return inputs;
	}

}