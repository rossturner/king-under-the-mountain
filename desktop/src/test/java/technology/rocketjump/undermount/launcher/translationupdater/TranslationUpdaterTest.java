package technology.rocketjump.undermount.launcher.translationupdater;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TranslationUpdaterTest {

	@Test
	public void fixReplacements() {
		String result = TranslationUpdater.fixReplacements("foop {{wrong}} foop {{also_wrong}} foop",
				"English {{replacements}} maintained {{correctly}}");

		assertThat(result).isEqualTo("foop {{replacements}} foop {{correctly}} foop");
	}

	@Test
	public void failingCase_singleBracket() {
		String english = "Used to bash small items of stone and metal into shape. One of the tools used by a {{PROFESSION.BLACKSMITH}}.";
		String german = "Wird verwendet, um kleine Gegenstände aus Stein und Metall in Form zu bringen. Eines der Werkzeuge, die von einem {{PROFESSION.BLACKSMITH} verwendet werden.";

		String result = TranslationUpdater.fixReplacements(german, english);

		assertThat(result).isEqualTo("Wird verwendet, um kleine Gegenstände aus Stein und Metall in Form zu bringen. Eines der Werkzeuge, die von einem {{PROFESSION.BLACKSMITH}} verwendet werden.");

	}

	@Test
	public void failingLongerCase() {
		String english = "Let’s get a {{ROOMS.KITCHEN}} set up to produce soup once your crops are ready. Go to the top " +
				"level menu then {{ZONES.LABEL}} > {{ROOMS.KITCHEN}} and drag out a {{ROOMS.KITCHEN}} area. At this point " +
				"in the development of King under the Mountain it does not matter if it is outside or indoors, but " +
				"in the future you’ll want all your rooms to have a ceiling to protect them from the elements.";

		String german = "Lassen Sie uns eine {{ROOMS.KITCHEN}} einrichten, um Suppe zu produzieren, sobald Ihre Ernte " +
				"fertig ist. Gehen Sie zum Menü der obersten Ebene, dann {{ZONES.LABEL}} > {{ROOMS.KÜCHE}} und ziehen " +
				"Sie einen {{ROOMS.KÜCHE}} Bereich heraus. An diesem Punkt in der Entwicklung von König unter dem Berg " +
				"spielt es keine Rolle, ob es draußen oder drinnen ist, aber in Zukunft werden Sie wollen, dass alle " +
				"Ihre Räume eine Decke haben, um sie vor den Elementen zu schützen.";

		String result = TranslationUpdater.fixReplacements(german, english);

		assertThat(result).isEqualTo("Lassen Sie uns eine {{ROOMS.KITCHEN}} einrichten, um Suppe zu produzieren, sobald Ihre Ernte " +
				"fertig ist. Gehen Sie zum Menü der obersten Ebene, dann {{ZONES.LABEL}} > {{ROOMS.KITCHEN}} und ziehen " +
				"Sie einen {{ROOMS.KITCHEN}} Bereich heraus. An diesem Punkt in der Entwicklung von König unter dem Berg " +
				"spielt es keine Rolle, ob es draußen oder drinnen ist, aber in Zukunft werden Sie wollen, dass alle " +
				"Ihre Räume eine Decke haben, um sie vor den Elementen zu schützen.");

	}
}