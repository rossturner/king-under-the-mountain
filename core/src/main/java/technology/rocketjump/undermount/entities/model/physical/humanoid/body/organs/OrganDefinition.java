package technology.rocketjump.undermount.entities.model.physical.humanoid.body.organs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.misc.Name;

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
}
