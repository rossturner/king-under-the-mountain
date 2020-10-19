package technology.rocketjump.undermount.persistence;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;

public class EnumParser {

	public static <T extends Enum<T>> T getEnumValue(JSONObject jsonObject, String key, Class<T> enumType, T defaultValue) throws InvalidSaveException {
		String enumName = jsonObject.getString(key);

		if (enumName == null) {
			return defaultValue;
		} else {
			T result = EnumUtils.getEnum(enumType, enumName);
			if (result == null) {
				throw new InvalidSaveException("Unrecognised " + enumType.getSimpleName() + ": " + enumName);
			}
			return result;
		}
	}
}
