package technology.rocketjump.undermount.ui.hints.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.misc.Name;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A hint is a dialog-like text prompt to act as a tutorial for the player
 * Each hint will be shown once (and only once) when any of its trigger conditions are met
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hint {

	@Name
	private String hintId; // Unique identifier

	private List<String> i18nKeys = new ArrayList<>();

	/*
	A list of things to show the progress of i.e. 2 of 4 wooden planks (in storage)
	Which when all are activated, automatically activates the first action of this Hint
	 */
	private List<HintProgressDescriptor> progressDescriptors = new ArrayList<>();

	private List<HintAction> actions = new ArrayList<>(); // Actions could be something like which buttons show on a hint e.g. "Next", "Don't show any hints"

	private List<HintTrigger> triggers = new ArrayList<>();

	private boolean dismissable = true; // Some more tutorial-like hints can't be dismissed

	public String getHintId() {
		return hintId;
	}

	public void setHintId(String hintId) {
		this.hintId = hintId;
	}

	public List<String> getI18nKeys() {
		return i18nKeys;
	}

	public void setI18nKeys(List<String> i18nKeys) {
		this.i18nKeys = i18nKeys;
	}

	public List<HintProgressDescriptor> getProgressDescriptors() {
		return progressDescriptors;
	}

	public void setProgressDescriptors(List<HintProgressDescriptor> progressDescriptors) {
		this.progressDescriptors = progressDescriptors;
	}

	public List<HintAction> getActions() {
		return actions;
	}

	public void setActions(List<HintAction> actions) {
		this.actions = actions;
	}

	public List<HintTrigger> getTriggers() {
		return triggers;
	}

	public void setTriggers(List<HintTrigger> triggers) {
		this.triggers = triggers;
	}

	public boolean isDismissable() {
		return dismissable;
	}

	public void setDismissable(boolean dismissable) {
		this.dismissable = dismissable;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Hint hint = (Hint) o;
		return hintId.equals(hint.hintId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hintId);
	}
}
