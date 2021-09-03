package technology.rocketjump.undermount.mapping.tile.underground;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.MoreObjects;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class UnderTile implements ChildPersistable {

	private Entity pipeEntity;

	private ChannelLayout channelLayout; // Channels either exist or don't, which is represented by the channelLayout existing

	private boolean liquidOutput;
	private boolean liquidInput;
	private TileLiquidFlow liquidFlow;

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
		if (liquidFlow != null) {
			stringHelper.add("liquidFlowAmount", liquidFlow.getLiquidAmount());
		}
		return stringHelper.toString();
	}

	public Entity getPipeEntity() {
		return pipeEntity;
	}

	public void setPipeEntity(Entity pipeEntity) {
		this.pipeEntity = pipeEntity;
	}

	public boolean isLiquidOutput() {
		return liquidOutput;
	}

	public void setLiquidOutput(boolean liquidOutput) {
		this.liquidOutput = liquidOutput;
	}

	public boolean isLiquidInput() {
		return liquidInput;
	}

	public void setLiquidInput(boolean liquidInput) {
		this.liquidInput = liquidInput;
	}

	public boolean liquidCanFlow() {
		// liquids flow through either pipes or channels
		return pipeEntity != null || channelLayout != null;
	}

	public TileLiquidFlow getLiquidFlow() {
		return liquidFlow;
	}

	public TileLiquidFlow getOrCreateLiquidFlow() {
		if (liquidFlow == null) {
			liquidFlow = new TileLiquidFlow();
		}
		return liquidFlow;
	}

	public void setLiquidFlow(TileLiquidFlow liquidFlow) {
		this.liquidFlow = liquidFlow;
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
		if (liquidOutput) {
			asJson.put("liquidOutput", true);
		}
		if (liquidInput) {
			asJson.put("liquidInput", true);
		}
		if (liquidFlow != null) {
			JSONObject liquidFlowJson = new JSONObject(true);
			liquidFlow.writeTo(liquidFlowJson, savedGameStateHolder);
			asJson.put("liquidFlow", liquidFlowJson);
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

		this.liquidOutput = asJson.getBooleanValue("liquidOutput");
		this.liquidInput = asJson.getBooleanValue("liquidInput");

		JSONObject liquidFlowJson = asJson.getJSONObject("liquidFlow");
		if (liquidFlowJson != null) {
			this.liquidFlow = new TileLiquidFlow();
			this.liquidFlow.readFrom(liquidFlowJson, savedGameStateHolder, relatedStores);
		}
	}

	public boolean liquidCanFlowFrom(MapTile sourceTile) {
		UnderTile sourceUnderTile = sourceTile.getUnderTile();
		// Following is to check that source and this both have a pipe or both have a channel
		return this.liquidCanFlow() && sourceUnderTile != null && sourceUnderTile.liquidCanFlow() &&
				((this.getPipeEntity() != null && sourceUnderTile.getPipeEntity() != null) ||
						(this.channelLayout != null && sourceUnderTile.getChannelLayout() != null));
	}
}
