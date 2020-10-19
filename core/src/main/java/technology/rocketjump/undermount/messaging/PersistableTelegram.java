package technology.rocketjump.undermount.messaging;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.msg.Telegram;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.messaging.types.EntityMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class PersistableTelegram extends Telegram implements ChildPersistable {

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (this.sender != null || this.receiver != null || returnReceiptStatus != 0) {
			throw new NotImplementedException("Not yet implemented, persisting telegrams with a sender or receiver");
		}

		asJson.put("messageType", this.message);

		float currentTime = GdxAI.getTimepiece().getTime();
		float delay = this.getTimestamp() - currentTime;
		asJson.put("delay", delay);

		if (extraInfo != null) {
			JSONObject extraInfoJson = new JSONObject(true);
			extraInfoJson.put("_class", extraInfo.getClass().getSimpleName());
			if (extraInfo instanceof Job) {
				Job job = (Job) extraInfo;
				extraInfoJson.put("jobId", job.getJobId());
			} else if (extraInfo instanceof EntityMessage) {
				EntityMessage entityMessage = (EntityMessage) extraInfo;
				extraInfoJson.put("entityId", entityMessage.getEntityId());
			} else {
				throw new NotImplementedException("Not yet implemented: persisting telegram with extraInfo of " + extraInfo.getClass().getSimpleName());
			}
			asJson.put("extraInfo", extraInfoJson);
		}

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.message = asJson.getIntValue("messageType");

		float delay = asJson.getFloatValue("delay");
		setTimestamp(GdxAI.getTimepiece().getTime() + delay);

		JSONObject extraInfoJson = asJson.getJSONObject("extraInfo");
		if (extraInfoJson != null) {
			String className = extraInfoJson.getString("_class");
			if (className.equals(Job.class.getSimpleName())) {
				Long jobId = extraInfoJson.getLong("jobId");
				Job job = savedGameStateHolder.jobs.get(jobId);
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobId);
				}
				this.extraInfo = job;
			} else if (className.equals(EntityMessage.class.getSimpleName())) {
				Long entityId = extraInfoJson.getLong("entityId");
				EntityMessage entityMessage = new EntityMessage(entityId);
				this.extraInfo = entityMessage;
			} else {
				throw new InvalidSaveException("Unrecognised telegram extrainfo class: " + className);
			}
		}
	}
}
