package technology.rocketjump.undermount.rooms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import technology.rocketjump.undermount.misc.Name;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockpileGroup {

	@Name
	private String name;
	@JsonIgnore
	private String i18nKey;

	public StockpileGroup() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.i18nKey = "STOCKPILE." + name.toUpperCase().replaceAll(" ", "_");
	}

	@JsonIgnore
	public String getI18nKey() {
		return i18nKey;
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
