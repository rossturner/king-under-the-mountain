package technology.rocketjump.undermount.ui.widgets;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class GameDialogDictionaryTest {

	@Test
	public void test_splitLines() {
		String[] result = GameDialogDictionary.splitLines("This is a long line where we are expecting a line break around the middle");

		assertThat(result[0]).isEqualTo("This is a long line where we are");
		assertThat(result[1]).isEqualTo("expecting a line break around the middle");
	}

}