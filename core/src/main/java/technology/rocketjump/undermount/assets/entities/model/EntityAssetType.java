package technology.rocketjump.undermount.assets.entities.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public class EntityAssetType {

	public final String name;

	public static final EntityAssetType NULL_ENTITY_ASSET_TYPE = new EntityAssetType("Null entity asset type");

	public EntityAssetType(String name) {
		this.name = name;
	}

	@JsonValue
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EntityAssetType that = (EntityAssetType) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}
