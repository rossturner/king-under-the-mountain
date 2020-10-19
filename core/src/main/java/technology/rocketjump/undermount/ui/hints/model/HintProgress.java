package technology.rocketjump.undermount.ui.hints.model;

import technology.rocketjump.undermount.ui.i18n.I18nText;

import java.util.Objects;

public class HintProgress {

	public final int current;
	public final int total;
	public final I18nText targetDescription;

	public HintProgress(int current, int total, I18nText targetDescription) {
		this.current = current;
		this.total = total;
		this.targetDescription = targetDescription;
	}

	public boolean isComplete() {
		return current >= total;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HintProgress progress = (HintProgress) o;
		return current == progress.current &&
				total == progress.total &&
				targetDescription.equals(progress.targetDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(current, total, targetDescription);
	}
}
