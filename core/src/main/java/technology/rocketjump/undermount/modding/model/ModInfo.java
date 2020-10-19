package technology.rocketjump.undermount.modding.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.misc.versioning.Version;

import java.util.Objects;

import static technology.rocketjump.undermount.misc.versioning.Version.Qualifier.Unknown;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModInfo {

	private String name;
	private String nameId;
	private String description;
	private Version version;
	private Version gameVersion; // specifies which version of the game this was created against for compatibility

	public String getName() {
		if (name == null) {
			return "No name specified";
		} else {
			return name;
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameId() {
		if (nameId != null) {
			return nameId;
		} else if (name != null) {
			return name.replace(" ", "-").toLowerCase();
		} else {
			return null;
		}
	}

	public void setNameId(String nameId) {
		this.nameId = nameId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Version getVersion() {
		if (version == null) {
			return new Version(Unknown.name() + " 0");
		} else {
			return version;
		}
	}

	public void setVersion(String version) {
		this.version = new Version(version);
	}

	public Version getGameVersion() {
		return gameVersion;
	}

	public void setGameVersion(String gameVersion) {
		this.gameVersion = new Version(gameVersion);
	}

	@Override
	public String toString() {
		return name + " " + version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ModInfo modInfo = (ModInfo) o;
		return Objects.equals(name, modInfo.name) &&
				Objects.equals(version, modInfo.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, version);
	}

	public boolean isBaseMod() {
		return getName().equals("base");
	}
}
