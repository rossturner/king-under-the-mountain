package technology.rocketjump.undermount.persistence.model;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;

public interface ChildPersistable {

	void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder);

	void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException;

}
