package technology.rocketjump.undermount.messaging;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class MessageTypeTest {

	@Test
	@SuppressWarnings("unchecked")
	public void testMessageTypeValueUniqueness() throws IllegalAccessException {
		Field[] messageTypeFields = MessageType.class.getFields();
		List valueList = new ArrayList();
		Set valueSet = new HashSet();
		MessageType instance = new MessageType();

		for (Field field : messageTypeFields) {
			Object value = field.get(instance);
			valueList.add(value);
			valueSet.add(value);
		}

		assertThat(valueSet).containsOnly(valueList.toArray());
		assertThat(valueSet.size()).isEqualTo(valueList.size());
	}

}