package technology.rocketjump.undermount.ui.widgets.tooltips;

import java.util.Objects;

public class I18nTextElement {

	private String text;
	private final String tooltipI18nKey;
	private boolean isLineBreak;

	public I18nTextElement(String text, String tooltipI18nKey) {
		this.text = text;
		this.tooltipI18nKey = tooltipI18nKey;
	}

	public static final I18nTextElement lineBreak = new I18nTextElement(null, null);
	static {
		lineBreak.isLineBreak = true;
	}

	public String getText() {
		if (isLineBreak) {
			return "\n";
		} else {
			return text;
		}
	}

	public String getTooltipI18nKey() {
		return tooltipI18nKey;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isLineBreak() {
		return isLineBreak;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		I18nTextElement that = (I18nTextElement) o;
		return Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {
		return text.hashCode();
	}

	@Override
	public String toString() {
		if (isLineBreak) {
			return "\n";
		}
		StringBuilder builder = new StringBuilder();
		builder.append(text);
		if (tooltipI18nKey != null) {
			builder.append(" {");
			if (tooltipI18nKey.length() <= 16) {
				builder.append(tooltipI18nKey);
			} else {
				builder.append(tooltipI18nKey, 0, 16);
			}
			builder.append("}");
		}
		return builder.toString();
	}
}
