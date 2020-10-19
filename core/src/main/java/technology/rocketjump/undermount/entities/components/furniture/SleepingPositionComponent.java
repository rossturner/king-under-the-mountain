package technology.rocketjump.undermount.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class SleepingPositionComponent implements EntityComponent {

	private EntityAssetOrientation sleepingOrientation;

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		SleepingPositionComponent cloned = new SleepingPositionComponent();
		cloned.setSleepingOrientation(this.sleepingOrientation);
		return cloned;
	}

	public EntityAssetOrientation getSleepingOrientation() {
		return sleepingOrientation;
	}

	public void setSleepingOrientation(EntityAssetOrientation sleepingOrientation) {
		this.sleepingOrientation = sleepingOrientation;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!EntityAssetOrientation.DOWN.equals(sleepingOrientation)) {
			asJson.put("orientation", sleepingOrientation.name());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.sleepingOrientation = EnumParser.getEnumValue(asJson, "orientation", EntityAssetOrientation.class, EntityAssetOrientation.DOWN);
	}
}
