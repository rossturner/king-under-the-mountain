package technology.rocketjump.undermount.settlement.notifications;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.widgets.IconButton;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Notification implements ChildPersistable {

	private long notificationId;
	private NotificationType type;
	private Vector2 worldPosition;
	private IconButton iconButton;
	private final Map<String, I18nText> textReplacements = new HashMap<>();

	public Notification() {

	}

	public Notification(NotificationType type, Vector2 worldPosition) {
		this.notificationId = SequentialIdGenerator.nextId();
		this.type = type;
		this.worldPosition = worldPosition;
	}

	public NotificationType getType() {
		return type;
	}

	public Vector2 getWorldPosition() {
		return worldPosition;
	}

	public long getNotificationId() {
		return notificationId;
	}

	public IconButton getIconButton() {
		return iconButton;
	}

	public void setIconButton(IconButton iconButton) {
		this.iconButton = iconButton;
	}

	public void addTextReplacement(String key, I18nText value) {
		textReplacements.put(key, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Notification that = (Notification) o;
		return notificationId == that.notificationId;
	}

	@Override
	public int hashCode() {
		return (int)notificationId;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("id", notificationId);
		asJson.put("type", type.name());
		if (worldPosition != null) {
			asJson.put("worldPosition", JSONUtils.toJSON(worldPosition));
		}

		if (!textReplacements.isEmpty()) {
			JSONObject replacementJson = new JSONObject(true);
			for (Map.Entry<String, I18nText> entry : textReplacements.entrySet()) {
				replacementJson.put(entry.getKey(), entry.getValue().toString());
			}
			asJson.put("replacements", replacementJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.notificationId = asJson.getLongValue("id");
		this.type = EnumParser.getEnumValue(asJson, "type", NotificationType.class, null);
		JSONObject worldPositionJson = asJson.getJSONObject("worldPosition");
		if (worldPositionJson != null) {
			this.worldPosition = JSONUtils.vector2(worldPositionJson);
		}

		JSONObject replacementJson = asJson.getJSONObject("replacements");
		if (replacementJson != null) {
			for (Map.Entry<String, Object> entry : replacementJson.entrySet()) {
				textReplacements.put(entry.getKey(), new I18nText((String) entry.getValue()));
			}
		}
	}

	public Set<Map.Entry<String, I18nText>> getTextReplacements() {
		return textReplacements.entrySet();
	}
}
