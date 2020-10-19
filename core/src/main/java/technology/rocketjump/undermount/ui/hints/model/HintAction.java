package technology.rocketjump.undermount.ui.hints.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HintAction {

	private HintActionType type;
	private String buttonTextI18nKey;
	private String relatedHintId;

	public enum HintActionType {

		DISABLE_TUTORIAL,
		DISABLE_ALL_HINTS,
		SHOW_OTHER_HINT,
		DISMISS

	}

	public HintActionType getType() {
		return type;
	}

	public void setType(HintActionType type) {
		this.type = type;
	}

	public String getButtonTextI18nKey() {
		return buttonTextI18nKey;
	}

	public void setButtonTextI18nKey(String buttonTextI18nKey) {
		this.buttonTextI18nKey = buttonTextI18nKey;
	}

	public String getRelatedHintId() {
		return relatedHintId;
	}

	public void setRelatedHintId(String relatedHintId) {
		this.relatedHintId = relatedHintId;
	}
}
