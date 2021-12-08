package technology.rocketjump.undermount.modding.exception;

public class ModLoadingException extends Exception {
	public ModLoadingException(Exception e) {
		super(e);
	}

	public ModLoadingException(String message) {
		super(message);
	}

	public ModLoadingException(String s, Exception e) {
		super(s, e);
	}
}
