package technology.rocketjump.undermount.misc.twitch;

import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidName;

import java.util.Objects;

public class TwitchViewer {

	private final String username;

	public TwitchViewer(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public HumanoidName toName() {
		HumanoidName name = new HumanoidName();
		if (username.contains("_")) {
			String[] parts = username.split("_");
			for (int cursor = 0; cursor < parts.length; cursor++) {
				String part = parts[cursor];
				StringBuilder surname = new StringBuilder();
				if (!part.isEmpty()) {
					if (name.getFirstName() == null) {
						name.setFirstName(WordUtils.capitalize(part));
					} else {
						if (surname.length() > 0) {
							surname.append(" ");
						}
						surname.append(WordUtils.capitalize(part));
					}
				}
				name.setLastName(surname.toString());
			}
		} else {
			name.setFirstName(WordUtils.capitalize(username));
		}
		return name;
	}

	@Override
	public String toString() {
		return toName().toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TwitchViewer that = (TwitchViewer) o;
		return Objects.equals(username, that.username);
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}
}
