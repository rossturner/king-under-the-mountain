package technology.rocketjump.undermount.rooms;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import technology.rocketjump.undermount.misc.Name;
import technology.rocketjump.undermount.rendering.utils.HexColors;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockpileGroup {

	@Name
	private String name;
	private String i18nKey;
	private String colorCode;
	@JsonIgnore
	private Color color = HexColors.POSITIVE_COLOR;

	public StockpileGroup() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
		if (colorCode != null) {
			this.color = HexColors.get(colorCode);
		}
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StockpileGroup that = (StockpileGroup) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
