package technology.rocketjump.undermount.ui.i18n;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;
import org.mockito.Mockito;
import technology.rocketjump.undermount.entities.EntityStore;
import technology.rocketjump.undermount.ui.widgets.tooltips.I18nTextElement;

import static org.fest.assertions.Assertions.assertThat;
import static technology.rocketjump.undermount.ui.i18n.LanguageType.DEFAULT_LINE_LENGTH;

public class I18nTextTest {

	@Test
	public void replacementTest() {
		I18nText original = new I18nText("Hello {{replace}} me");

		original.replace("{{replace}}", "word", "TOOLTIP.KEY");

		assertThat(original.getElements()).hasSize(3);
		assertThat(original.toString()).isEqualTo("Hello word me");
		assertThat(original.getElements().get(0).getText()).isEqualTo("Hello ");
		assertThat(original.getElements().get(1).getText()).isEqualTo("word");
		assertThat(original.getElements().get(2).getText()).isEqualTo(" me");

		assertThat(original.getElements().get(0).getTooltipI18nKey()).isNull();
		assertThat(original.getElements().get(1).getTooltipI18nKey()).isEqualTo("TOOLTIP.KEY");
		assertThat(original.getElements().get(2).getTooltipI18nKey()).isNull();
	}

	@Test
	public void tidy_stripsMultipleSpaces() {
		I18nText original = new I18nText("Hello {{replace}} me");

		original.replace("{{replace}}", "", null);
		original.tidy(false);

		assertThat(original.getElements()).hasSize(2);
		assertThat(original.toString()).isEqualTo("Hello me");
		assertThat(original.getElements().get(0).getText()).isEqualTo("Hello ");
		assertThat(original.getElements().get(1).getText()).isEqualTo("me");
	}

	@Test
	public void breakAfterLength_doesNotBreakLessThan5CharsToNewLine() {
		I18nText original = new I18nText("A lightly-coloured sedimentary rock which makes for a good building material. It is also used as a flux in the production of {{STEEL}}.");
		original.replace("{{STEEL}}", "steel", "TOOLTIP.KEY");
		original.breakAfterLength(60);

		assertThat(original.getElements()).hasSize(5);
		assertThat(original.getElements().get(4).getText()).isEqualTo(".");
	}

	@Test
	public void breakAfterLength_breaksUpLongLines() {
		I18nText original = new I18nText("Dwarves are short and stocky humanoids. They are masters of working with stone and metal. Most dwarves distrust the sky and prefer to stay underground.");
		original.breakAfterLength(60);

		assertThat(original.toString()).isEqualTo("Dwarves are short and stocky humanoids. They are masters of working\nwith stone and metal. Most dwarves distrust the sky and prefer\nto stay underground.");
	}


	@Test
	public void breakAfterLength_breaksOnJapaneseFullStop() {
		I18nText original = new I18nText("大きな掘り出し出しエリアが崩壊するのを止めるには、柱が必要です。柱やその他の支柱なしで採掘された7x7タイルの面積は、最終的に崩壊するでしょう。");
		original.breakAfterLength(20);

		assertThat(original.toString()).isEqualTo("大きな掘り出し出しエリアが崩壊するのを止めるには、\n" +
				"柱が必要です。柱やその他の支柱なしで採掘された7x7タイルの面積は、\n" +
				"最終的に崩壊するでしょう。");
	}

	@Test
	public void breakAfterLength_trimsStartOfNextElement() {
		I18nText original = new I18nText("A starchy root vegetable which can survive {{SEASON.WINTER}} making it a good choice for most settlements. Can be planted in {{SEASON.SPRING}}, {{SEASON.SUMMER}} or {{SEASON.AUTUMN}}.");
		original.replace("{{SEASON.WINTER}}", "winter", null);
		original.replace("{{SEASON.SPRING}}", "spring", null);
		original.replace("{{SEASON.SUMMER}}", "summer", null);
		original.replace("{{SEASON.AUTUMN}}", "autumn", null);

		original.breakAfterLength(60);

		assertThat(original.toString()).isEqualTo("A starchy root vegetable which can survive winter making it a\ngood choice for most settlements. Can be planted in spring, summer\nor autumn.");
	}

	@Test
	public void replace_withRecursiveReplacement() {
		I18nText original = new I18nText("Hello {{replace}} me");

		original.replace("{{replace}}", "another {{replacement}} string", "TOOLTIP.KEY");
		original.replace("{{replacement}}", "final", null);

		assertThat(original.getElements()).hasSize(5);
		assertThat(original.toString()).isEqualTo("Hello another final string me");
		assertThat(original.getElements().get(0).getText()).isEqualTo("Hello ");
		assertThat(original.getElements().get(1).getText()).isEqualTo("another ");
		assertThat(original.getElements().get(2).getText()).isEqualTo("final");
	}

	@Test
	public void replace_withOtherI18nText() {
		I18nText original = new I18nText("Hello {{replace}} me");
		I18nText replacement = new I18nText("one {{two}} three");
		replacement.replace("{{two}}", "2", null);
		assertThat(replacement.getElements()).hasSize(3);

		original.replace("{{replace}}", replacement);

		assertThat(original.toString()).isEqualTo("Hello one 2 three me");
		assertThat(original.getElements()).hasSize(5);
	}

	@Test
	public void breakAfterLength_insertsLineBreaks_afterSpecifiedCharacters() {
		I18nText original = new I18nText("Here are some words with several characters that should get split multiple times");

		original.breakAfterLength(10);

		assertThat(original.toString()).isEqualTo("Here are some\n" +
				"words with\n" +
				"several characters\n" +
				"that should\n" +
				"get split multiple\n" +
				"times");
		assertThat(original.getElements()).hasSize(11);
	}

	@Test
	public void breakAfterLength_breaksAroundReplacements() {
		Injector injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(EntityStore.class).toInstance(Mockito.mock(EntityStore.class));
			}
		});
		I18nTranslator i18nTranslator = injector.getInstance(I18nTranslator.class);

		I18nText text = i18nTranslator.getTranslatedString("TUTORIAL.2.C").breakAfterLength(DEFAULT_LINE_LENGTH);

		// Previously this did not insert a line break
		assertThat(text.getElements().get(0).getText()).isEqualTo("For the seeds to actually be available to your farmers, they need to be in a stockpile compared to being held in");
		assertThat(text.getElements().get(1).isLineBreak()).isTrue();
	}


	@Test
	public void breakOnNewLine_insertsLineBreaks_atSpecifiedCharacters() {
		I18nText original = new I18nText("Here are\nsome words");
		assertThat(original.getElements()).hasSize(1);

		original.tidy(false);

		assertThat(original.toString()).isEqualTo("Here are\nsome words");
		assertThat(original.getElements()).hasSize(3);
		assertThat(original.getElements().get(0).getText()).isEqualTo("Here are");
		assertThat(original.getElements().get(1)).isEqualTo(I18nTextElement.lineBreak);
		assertThat(original.getElements().get(2).getText()).isEqualTo("some words");
	}

	@Test
	public void equals_returnsEquality_forDifferentInstancesWithSameText() {
		I18nText original = new I18nText("Here are\nsome words");
		I18nText other = new I18nText("Here are\nsome words");

		assertThat(original).isEqualTo(other);
	}

}