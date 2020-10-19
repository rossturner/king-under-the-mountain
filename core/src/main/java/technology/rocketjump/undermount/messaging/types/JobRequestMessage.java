package technology.rocketjump.undermount.messaging.types;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.planning.JobAssignmentCallback;
import technology.rocketjump.undermount.environment.GameClock;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.Persistable;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class JobRequestMessage implements Persistable {

	private long requestId;
	private double requestedAtTime;
	private JobAssignmentCallback callback; // This is not persisted, instead the SelectJobAction which references this sets it again
	private Entity requestingEntity;
	private boolean cancelled;

	public JobRequestMessage() {

	}

	public JobRequestMessage(Entity requestingEntity, GameClock gameClock, JobAssignmentCallback callback) {
		this.requestingEntity = requestingEntity;
		this.callback = callback;
		this.requestId = SequentialIdGenerator.nextId();
		this.requestedAtTime = gameClock.getCurrentGameTime();
	}

	public JobAssignmentCallback getCallback() {
		return callback;
	}

	public Entity getRequestingEntity() {
		return requestingEntity;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public long getRequestId() {
		return requestId;
	}

	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	public double getRequestedAtTime() {
		return requestedAtTime;
	}

	public void setRequestedAtTime(double requestedAtTime) {
		this.requestedAtTime = requestedAtTime;
	}

	public void setCallback(JobAssignmentCallback callback) {
		this.callback = callback;
	}

	public void setRequestingEntity(Entity requestingEntity) {
		this.requestingEntity = requestingEntity;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.jobRequests.containsKey(requestId)) {
			return;
		}
		JSONObject asJson = new JSONObject(true);
		asJson.put("id", requestId);
		asJson.put("requestedAt", requestedAtTime);
		// Don't write requesterId, this is handled via init()
		if (cancelled) {
			asJson.put("cancelled", true);
		}

		savedGameStateHolder.jobRequests.put(requestId, this);
		savedGameStateHolder.jobRequestsJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		requestId = asJson.getLongValue("id");
		requestedAtTime = asJson.getDoubleValue("requestedAt");
		cancelled = asJson.getBooleanValue("cancelled");

		savedGameStateHolder.jobRequests.put(requestId, this);
	}
}
