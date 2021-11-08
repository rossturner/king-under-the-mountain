package technology.rocketjump.undermount.entities.model.physical.creature;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class HumanoidName implements ChildPersistable {

	private String firstName;
	private String lastName;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void rename(String newName) {
		int spaceIndex = newName.indexOf(" ");
		if (spaceIndex != -1) {
			firstName = newName.substring(0, spaceIndex);
			lastName = newName.substring(spaceIndex + 1);
		} else {
			firstName = newName;
			lastName = "";
		}
	}

	@Override
	public String toString() {
		if (lastName == null || lastName.isEmpty()) {
			return firstName;
		} else {
			return firstName + " " + lastName;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("firstName", firstName);
		asJson.put("lastName", lastName);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		firstName = asJson.getString("firstName");
		lastName = asJson.getString("lastName");
	}
}
