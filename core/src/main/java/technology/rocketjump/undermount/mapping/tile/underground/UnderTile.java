package technology.rocketjump.undermount.mapping.tile.underground;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.MoreObjects;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.persistence.EnumParser;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class UnderTile implements ChildPersistable {

	private PipeConstructionState pipeConstructionState = PipeConstructionState.NONE;
	private Entity pipeEntity;

	private ChannelLayout channelLayout; // Channels either exist or don't, which is represented by the channelLayout existing

	private boolean liquidSource;
	private boolean liquidConsumer;
	private TileLiquidFlow liquidFlow;

	private boolean powerSource;
	private boolean powerConsumer;

	private MechanismType queuedMechanismType;
	private Entity powerMechanismEntity;

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

	public boolean isLiquidConsumer() {
		return liquidConsumer;
	}

	public void setLiquidConsumer(boolean liquidConsumer) {
		this.liquidConsumer = liquidConsumer;
	}

	public boolean isLiquidSource() {
		return liquidSource;
	}

	public void setLiquidSource(boolean liquidSource) {
		this.liquidSource = liquidSource;
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

	public PipeConstructionState getPipeConstructionState() {
		return pipeConstructionState;
	}

	public void setPipeConstructionState(PipeConstructionState pipeConstructionState) {
		this.pipeConstructionState = pipeConstructionState;
	}

	public boolean isPowerSource() {
		return powerSource;
	}

	public void setPowerSource(boolean powerSource) {
		this.powerSource = powerSource;
	}

	public boolean isPowerConsumer() {
		return powerConsumer;
	}

	public void setPowerConsumer(boolean powerConsumer) {
		this.powerConsumer = powerConsumer;
	}

	public MechanismType getQueuedMechanismType() {
		return queuedMechanismType;
	}

	public void setQueuedMechanismType(MechanismType queuedMechanismType) {
		this.queuedMechanismType = queuedMechanismType;
	}

	public Entity getPowerMechanismEntity() {
		return powerMechanismEntity;
	}

	public void setPowerMechanismEntity(Entity powerMechanismEntity) {
		this.powerMechanismEntity = powerMechanismEntity;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (channelLayout != null) {
			asJson.put("channelLayout", channelLayout.getId());
		}
		if (!pipeConstructionState.equals(PipeConstructionState.NONE)) {
			asJson.put("pipeConstructionState", pipeConstructionState.name());
		}
		if (pipeEntity != null) {
			pipeEntity.writeTo(savedGameStateHolder);
			asJson.put("pipeEntity", pipeEntity.getId());
		}
		if (liquidSource) {
			asJson.put("liquidSource", true);
		}
		if (liquidConsumer) {
			asJson.put("liquidConsumer", true);
		}
		if (liquidFlow != null) {
			JSONObject liquidFlowJson = new JSONObject(true);
			liquidFlow.writeTo(liquidFlowJson, savedGameStateHolder);
			asJson.put("liquidFlow", liquidFlowJson);
		}

		if (powerSource) {
			asJson.put("powerSource", true);
		}
		if (powerConsumer) {
			asJson.put("powerConsumer", true);
		}

		if (queuedMechanismType != null) {
			asJson.put("queuedMechanismType", queuedMechanismType.getName());
		}

		if (powerMechanismEntity != null) {
			powerMechanismEntity.writeTo(savedGameStateHolder);
			asJson.put("powerMechanismEntity", powerMechanismEntity.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Integer channelLayoutId = asJson.getInteger("channelLayout");
		if (channelLayoutId != null) {
			this.channelLayout = new ChannelLayout(channelLayoutId);
		}

		this.pipeConstructionState = EnumParser.getEnumValue(asJson, "pipeConstructionState", PipeConstructionState.class, PipeConstructionState.NONE);

		Long pipeEntityId = asJson.getLong("pipeEntity");
		if (pipeEntityId != null) {
			this.pipeEntity = savedGameStateHolder.entities.get(pipeEntityId);
		}

		this.liquidSource = asJson.getBooleanValue("liquidSource");
		this.liquidConsumer = asJson.getBooleanValue("liquidConsumer");

		JSONObject liquidFlowJson = asJson.getJSONObject("liquidFlow");
		if (liquidFlowJson != null) {
			this.liquidFlow = new TileLiquidFlow();
			this.liquidFlow.readFrom(liquidFlowJson, savedGameStateHolder, relatedStores);
		}

		this.powerSource = asJson.getBooleanValue("powerSource");
		this.powerConsumer = asJson.getBooleanValue("powerConsumer");

		String queuedMechanismTypeName = asJson.getString("queuedMechanismType");
		if (queuedMechanismTypeName != null) {
			this.queuedMechanismType = relatedStores.mechanismTypeDictionary.getByName(queuedMechanismTypeName);
			if (this.queuedMechanismType == null) {
				throw new InvalidSaveException("Could not find mechanism type " + queuedMechanismTypeName);
			}
		}

		Long powerMechanismEntityId = asJson.getLong("powerMechanismEntity");
		if (powerMechanismEntityId != null) {
			this.powerMechanismEntity = savedGameStateHolder.entities.get(powerMechanismEntityId);
			if (this.powerMechanismEntity == null) {
				throw new InvalidSaveException("Could not find mechanism entity with ID " + powerMechanismEntityId);
			}
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
