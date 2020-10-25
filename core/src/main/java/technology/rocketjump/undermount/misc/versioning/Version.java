package technology.rocketjump.undermount.misc.versioning;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * This class represents the constituent parts of a game version e.g. "Alpha 2.1", "Beta 3", "1.0"
 * or a mod version using Major.Minor.Patch semantic versioning
 */
public class Version {

	public final Qualifier qualifier; // e.g. Alpha, Beta, Other
	public final int major;
	public final int minor;
	public final int revision;

	public Version(String versionText) {
		if (versionText == null) {
			throw new IllegalArgumentException("Text provided to " + getClass().getSimpleName() + " must not be null");
		}

		Qualifier qualifier = Qualifier.Unspecified;
		if (!StringUtils.isNumeric(versionText.substring(0, 1))) {
			for (Qualifier qualifierInstance : Qualifier.values()) {
				if (StringUtils.startsWith(versionText.toUpperCase(), qualifierInstance.name().toUpperCase())) {
					qualifier = qualifierInstance;
					versionText = versionText.substring(qualifier.name().length() + 1);
					break;
				}
			}

			if (qualifier.equals(Qualifier.Unspecified)) {
				throw new IllegalArgumentException("Unrecognised qualifier before version string " + versionText);
			}
		}
		this.qualifier = qualifier;

		String[] versionParts = versionText.split("\\.");

		this.major = Integer.parseInt(versionParts[0].trim());

		if (versionParts.length > 1) {
			this.minor = Integer.parseInt(versionParts[1].trim());
		} else {
			this.minor = 0;
		}
		if (versionParts.length > 2) {
			this.revision = Integer.parseInt(versionParts[2].trim());
		} else {
			this.revision = 0;
		}

		if (versionParts.length > 3) {
			throw new IllegalArgumentException("Illegal format for version text, must be in major.minor.revision format, was " + versionText);
		}
	}

	private final int NUM_SEPARATOR = 1000;
	public int toInteger() {
		return (qualifier.modifier * NUM_SEPARATOR * NUM_SEPARATOR * NUM_SEPARATOR) +
				(major * NUM_SEPARATOR * NUM_SEPARATOR) +
				(minor * NUM_SEPARATOR) +
				revision;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		if (!qualifier.equals(Qualifier.Unspecified)) {
			result.append(qualifier.name()).append(" ");
		}
		result.append(major);
		if (minor > 0 || revision > 0) {
			result.append(".").append(minor);
		}
		if (revision > 0) {
			result.append(".").append(revision);
		}
		return result.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Version version = (Version) o;
		return major == version.major &&
				minor == version.minor &&
				revision == version.revision &&
				qualifier == version.qualifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(qualifier, major, minor, revision);
	}

	public enum Qualifier {

		Unknown(-1),

		Alpha(0),
		Beta(1),
		Unspecified(2);

		public final int modifier;

		Qualifier(int modifier) {
			this.modifier = modifier;
		}
	}

	public enum Scheme {

		GAME_VERSION,
		SEMANTIC_VERSIONING

	}

}
