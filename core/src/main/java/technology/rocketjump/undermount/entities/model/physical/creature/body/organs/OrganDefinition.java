package technology.rocketjump.undermount.entities.model.physical.creature.body.organs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.misc.Name;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganDefinition {

	@Name
	private String name;
	private OrganFunction function;
	private OrganDamageMapping damage;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public OrganFunction getFunction() {
		return function;
	}

	public void setFunction(OrganFunction function) {
		this.function = function;
	}

	public OrganDamageMapping getDamage() {
		return damage;
	}

	public void setDamage(OrganDamageMapping damage) {
		this.damage = damage;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OrganDefinition that = (OrganDefinition) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
