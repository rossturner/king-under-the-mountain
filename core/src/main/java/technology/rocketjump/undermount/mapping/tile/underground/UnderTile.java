package technology.rocketjump.undermount.mapping.tile.underground;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.MoreObjects;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class UnderTile implements ChildPersistable {

	private Entity pipeEntity;

	private ChannelLayout channelLayout; // Channels either exist or don't, which is represented by the channelLayout existing

	public ChannelLayout getChannelLayout() {
		return channelLayout;
	}

	public void setChannelLayout(ChannelLayout channelLayout) {
		this.channelLayout = channelLayout;
	}

	@Override
	public String toString() {
		MoreObjects.ToStringHelper stringHelper = MoreObjects.toStringHelper(this);
		if (channelLayout != null) {
			stringHelper.add("channelLayout", channelLayout.getId()+"\n"+channelLayout.toString());
		}
		if (pipeEntity != null) {
			MechanismEntityAttributes attributes = (MechanismEntityAttributes) pipeEntity.getPhysicalEntityComponent().getAttributes();
			PipeLayout pipeLayout = attributes.getPipeLayout();
			stringHelper.add("pipe", pipeLayout.getId()+"\n"+pipeLayout);
		}
		return stringHelper.toString();
	}

	public Entity getPipeEntity() {
		return pipeEntity;
	}

	public void setPipeEntity(Entity pipeEntity) {
		this.pipeEntity = pipeEntity;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (channelLayout != null) {
			asJson.put("channelLayout", channelLayout.getId());
		}
		if (pipeEntity != null) {
			pipeEntity.writeTo(savedGameStateHolder);
			asJson.put("pipeEntity", pipeEntity.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Integer channelLayoutId = asJson.getInteger("channelLayout");
		if (channelLayoutId != null) {
			this.channelLayout = new ChannelLayout(channelLayoutId);
		}

		Long pipeEntityId = asJson.getLong("pipeEntity");
		if (pipeEntityId != null) {
			this.pipeEntity = savedGameStateHolder.entities.get(pipeEntityId);
		}
	}

}
