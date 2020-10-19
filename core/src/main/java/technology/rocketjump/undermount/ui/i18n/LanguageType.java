package technology.rocketjump.undermount.ui.i18n;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.misc.Name;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LanguageType {

	public static final int DEFAULT_LINE_LENGTH = 120;

	private String label;
	private String labelEn;
	@Name
	private String code;
	private String icon;
	@JsonIgnore
	private Sprite iconSprite;
	private String fallback;
	private String filename;
	private boolean enabled = true;
	private int breakAfterLineLength = DEFAULT_LINE_LENGTH;
	private String fontName;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabelEn() {
		return labelEn;
	}

	public void setLabelEn(String labelEn) {
		this.labelEn = labelEn;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public Sprite getIconSprite() {
		return iconSprite;
	}

	public void setIconSprite(Sprite iconSprite) {
		this.iconSprite = iconSprite;
	}

	public String getFallback() {
		return fallback;
	}

	public void setFallback(String fallback) {
		this.fallback = fallback;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public String toString() {
		return label;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getBreakAfterLineLength() {
		return breakAfterLineLength;
	}

	public void setBreakAfterLineLength(int breakAfterLineLength) {
		this.breakAfterLineLength = breakAfterLineLength;
	}

	public String getFontName() {
		return fontName;
	}

	public void setFontName(String fontName) {
		this.fontName = fontName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LanguageType that = (LanguageType) o;

		return labelEn != null ? labelEn.equals(that.labelEn) : that.labelEn == null;
	}

	@Override
	public int hashCode() {
		return labelEn != null ? labelEn.hashCode() : 0;
	}
}
