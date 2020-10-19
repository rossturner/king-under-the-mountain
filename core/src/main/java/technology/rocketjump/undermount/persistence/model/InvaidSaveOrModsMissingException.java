package technology.rocketjump.undermount.persistence.model;

import java.util.List;

public class InvaidSaveOrModsMissingException extends InvalidSaveException {

	public final List<String> missingModNames;

	public InvaidSaveOrModsMissingException(List<String> missingModNames, String message) {
		super(message);
		this.missingModNames = missingModNames;
	}
}
