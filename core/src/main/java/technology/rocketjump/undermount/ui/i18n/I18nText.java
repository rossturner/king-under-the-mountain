package technology.rocketjump.undermount.ui.i18n;

import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.undermount.ui.widgets.tooltips.I18nTextElement;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents an already-translated string split into parts, with each part potentially having tooltip text (also translated)
 */
public class I18nText implements I18nString {

	public static final I18nText BLANK = new I18nText("");

	private List<I18nTextElement> textElements = new ArrayList<>();

	public I18nText(String string) {
		textElements.add(new I18nTextElement(string, null));
	}

	public I18nText(String text, String tooltipI18nKey) {
		textElements.add(new I18nTextElement(text, tooltipI18nKey));
	}

	public I18nText append(I18nString other) {
		if (other instanceof I18nText) {
			I18nText otherText = (I18nText) other;
			this.textElements.addAll(otherText.textElements);
		} else if (other instanceof  I18nWord) {
			I18nWord otherString = (I18nWord) other;
			this.append(new I18nText(otherString.toString(),
					otherString.hasTooltip() ? otherString.get(I18nWordClass.TOOLTIP) : null));
		} else {
			this.textElements.add(new I18nTextElement(other.toString(), null));
		}
		return this.tidy(true);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (I18nTextElement textElement : textElements) {
			builder.append(textElement.getText());
		}
		return builder.toString();
	}

	public List<I18nTextElement> getElements() {
		return textElements;
	}

	public String getFirstTooltip() {
		for (I18nTextElement textElement : textElements) {
			if (textElement.getTooltipI18nKey() != null) {
				return textElement.getTooltipI18nKey();
			}
		}
		return null;
	}

	public void replace(String textToReplace, String replacementText, String tooltipI18nKey) {
		for (int cursor = 0; cursor < textElements.size(); cursor++) {
			I18nTextElement textElement = textElements.get(cursor);
			String elementText = textElement.getText();
			if (elementText.contains(textToReplace)) {

				String prefix = elementText.substring(0, elementText.indexOf(textToReplace));
				String suffix = elementText.substring(elementText.indexOf(textToReplace) + textToReplace.length());

				textElements.add(cursor, new I18nTextElement(suffix, textElement.getTooltipI18nKey()));
				if (!StringUtils.isBlank(replacementText)) {
					textElements.add(cursor, new I18nTextElement(replacementText, tooltipI18nKey));
					textElements.add(cursor, new I18nTextElement(prefix, textElement.getTooltipI18nKey()));
					textElements.remove(cursor + 3);
				} else {
					textElements.add(cursor, new I18nTextElement(prefix, textElement.getTooltipI18nKey()));
					textElements.remove(cursor + 2);
				}

				break;
			}
		}
	}

	public void replace(String textToReplace, I18nText replacement) {
		for (int cursor = 0; cursor < textElements.size(); cursor++) {
			I18nTextElement textElement = textElements.get(cursor);
			String elementText = textElement.getText();
			if (elementText.contains(textToReplace)) {

				String prefix = elementText.substring(0, elementText.indexOf(textToReplace));
				String suffix = elementText.substring(elementText.indexOf(textToReplace) + textToReplace.length());

				textElements.add(cursor, new I18nTextElement(suffix, textElement.getTooltipI18nKey()));
				for (int replacementCursor = replacement.getElements().size() - 1; replacementCursor >= 0; replacementCursor--) {
					textElements.add(cursor, replacement.getElements().get(replacementCursor));
				}
				textElements.add(cursor, new I18nTextElement(prefix, textElement.getTooltipI18nKey()));
				textElements.remove(cursor + 2 + (replacement.textElements.size()));

				break;
			}
		}
	}

	public I18nText tidy(boolean firstInvocation) {
		if (firstInvocation && textElements.size() > 1 && textElements.stream().allMatch(e -> e.getTooltipI18nKey() == null)) {
			// no tooltips, merge all text together
			String combinedText = textElements.stream().map(I18nTextElement::getText).collect(Collectors.joining());
			combinedText = combinedText.replaceAll(" +", " ");
			textElements.clear();
			textElements.add(new I18nTextElement(combinedText, null));
		}

		boolean initialCapitalised = false;
		boolean previousEndedWithSpace = false;
		ListIterator<I18nTextElement> iterator = textElements.listIterator();
		while (iterator.hasNext()) {
			I18nTextElement textElement = iterator.next();
			if (textElement.getText().length() == 0) {
				iterator.remove();
				continue;
			}

			if (!initialCapitalised) {
				textElement.setText(StringUtils.capitalize(textElement.getText()));
				initialCapitalised = true;
			}

			if (previousEndedWithSpace && textElement.getText().startsWith(" ")) {
				textElement.setText(textElement.getText().substring(1));

				// Might now be empty
				if (textElement.getText().length() == 0) {
					iterator.remove();
					continue;
				}
			}

			previousEndedWithSpace = textElement.getText().charAt(textElement.getText().length() - 1) == ' ';
		}


		// Replace any line breaks
		for (int cursor = 0; cursor < textElements.size(); cursor++) {
			I18nTextElement element = textElements.get(cursor);
			if (element.isLineBreak()) {
				continue;
			}
			int newLineIndex = element.getText().indexOf('\n');
			if (newLineIndex != -1) {
				insertLineBreakAt(newLineIndex, cursor, element);
				// Rescursively call this
				tidy(false);
				return this;
			}
		}

		return this;
	}

	public I18nText breakAfterLength(int lineLength) {
		return breakAfterLength(lineLength, 0);
	}

	private static final List<Character> breakChars = Arrays.asList(' ', '。', '、');

	private I18nText breakAfterLength(int lineLength, int initialCursor) {
		int currentLineLength = 0;
		for (int cursor = initialCursor; cursor < textElements.size(); cursor++) {
			I18nTextElement element = textElements.get(cursor);
			if (element.isLineBreak()) {
				currentLineLength = 0;
				continue;
			}

			boolean previousElementIsLineBreak = cursor >  0 && textElements.get(cursor - 1).isLineBreak();
			if (previousElementIsLineBreak && element.getText().startsWith(" ")) {
				element.setText(element.getText().substring(1));
			}
			// Aim to skip line break if next element is the last one and quite short.
			boolean nextElementIsLastAndShortLength = false;
			if (cursor + 1 == textElements.size() - 1 && textElements.get(cursor + 1).getText().length() < 5) {
				nextElementIsLastAndShortLength = true;
			}
			boolean currentElementIsLastAndShort = false;
			if (cursor == textElements.size() - 1 && element.getText().length() < 5) {
				currentElementIsLastAndShort = true;
			}

			if (currentLineLength + element.getText().length() > lineLength && !nextElementIsLastAndShortLength && !currentElementIsLastAndShort) {
				// Insert linebreak at space after currentLineLength + element length
				int breakAfter = lineLength - currentLineLength;
				int lineBreakPosition = -1;
				for (int elementCursor = 0; elementCursor < element.getText().length(); elementCursor++) {
					if (breakAfter > 0) {
						breakAfter--;
					} else {
						// Can now break
						char cursorChar = element.getText().charAt(elementCursor);
						if (breakChars.contains(cursorChar)) {
							lineBreakPosition = elementCursor;
							break;
						}
					}
				}

				if (lineBreakPosition != -1) {
					insertLineBreakAt(lineBreakPosition, cursor, element);
				} else {
					// add linebreak after current element
					textElements.add(cursor + 1, I18nTextElement.lineBreak);
				}
				// Recursively call this to start again
				return breakAfterLength(lineLength, cursor + 2);

			} else {
				currentLineLength += element.getText().length();
			}
		}


		return this;
	}

	private void insertLineBreakAt(int lineBreakPosition, int elementsCursor, I18nTextElement element) {
		String prefix = element.getText().substring(0, lineBreakPosition);
		String suffix = element.getText().substring(lineBreakPosition + 1);
		if (suffix.isEmpty() && elementsCursor == this.textElements.size() - 1) {
			return;
		}
		char lineBreakCharacter = element.getText().charAt(lineBreakPosition);
		if (lineBreakCharacter != ' ' && lineBreakCharacter != '\n') {
			// Include non-space characters
			prefix = element.getText().substring(0, lineBreakPosition + 1);
		}

		int addedElements = 0;
		if (suffix.length() > 0) {
			textElements.add(elementsCursor, new I18nTextElement(suffix, element.getTooltipI18nKey()));
			addedElements++;
		}
		textElements.add(elementsCursor, I18nTextElement.lineBreak);
		textElements.add(elementsCursor, new I18nTextElement(prefix, element.getTooltipI18nKey()));
		addedElements += 2;
		textElements.remove(elementsCursor + addedElements);
	}

	public boolean isEmpty() {
		return textElements.isEmpty() || textElements.stream().allMatch(e -> e.getText().isEmpty());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		I18nText i18nText = (I18nText) o;
		return Objects.equals(textElements, i18nText.textElements);
	}

	@Override
	public int hashCode() {
		return Objects.hash(textElements);
	}

}
