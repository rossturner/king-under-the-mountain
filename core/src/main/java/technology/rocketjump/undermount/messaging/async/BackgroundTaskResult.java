package technology.rocketjump.undermount.messaging.async;

public class BackgroundTaskResult {

	public final boolean dispatchMessageOnSuccess;
	public final ErrorType error;
	public final int successMessageType;
	public final Object successMessagePayload;

	public static BackgroundTaskResult success() {
		return new BackgroundTaskResult(false, -1, null, null);
	}

	public static BackgroundTaskResult success(int successMessageType, Object successMessagePayload) {
		return new BackgroundTaskResult(true, successMessageType, successMessagePayload, null);
	}


	public static BackgroundTaskResult error(ErrorType errorType) {
		return new BackgroundTaskResult(false, -1, null, errorType);
	}

	private BackgroundTaskResult(boolean dispatchMessageOnSuccess, int successMessageType, Object successMessagePayload, ErrorType error) {
		this.dispatchMessageOnSuccess = dispatchMessageOnSuccess;
		this.error = error;
		this.successMessageType = successMessageType;
		this.successMessagePayload = successMessagePayload;
	}

	public boolean isSuccessful() {
		return error == null;
	}

}
