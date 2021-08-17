package technology.rocketjump.undermount.mapping.tile.underground;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.MoreObjects;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class UnderTile implements ChildPersistable {

	private Entity pipeEntity;
	private GameMaterial pipeMaterial;
	private PipeLayout pipeLayout;

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
		if (pipeLayout != null) {
			stringHelper.add("pipeLayout", pipeLayout.getId()+"\n"+pipeLayout.toString());
		}
		return stringHelper.toString();
	}

	public GameMaterial getPipeMaterial() {
		return pipeMaterial;
	}

	public void setPipeMaterial(GameMaterial pipeMaterial) {
		this.pipeMaterial = pipeMaterial;
	}

	public PipeLayout getPipeLayout() {
		return pipeLayout;
	}

	public void setPipeLayout(PipeLayout pipeLayout) {
		this.pipeLayout = pipeLayout;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (channelLayout != null) {
			asJson.put("channelLayout", channelLayout.getId());
		}
		if (pipeLayout != null) {
			asJson.put("pipeLayout", pipeLayout.getId());
			asJson.put("pipeMaterial", pipeMaterial.getMaterialName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Integer channelLayoutId = asJson.getInteger("channelLayout");
		if (channelLayoutId != null) {
			this.channelLayout = new ChannelLayout(channelLayoutId);
		}

		Integer pipeLayoutId = asJson.getInteger("pipeLayout");
		if (pipeLayoutId != null) {
			this.pipeLayout = new PipeLayout(pipeLayoutId);
			this.pipeMaterial = relatedStores.gameMaterialDictionary.getByName(asJson.getString("pipeMaterial"));
			if (this.pipeMaterial == null) {
				throw new InvalidSaveException("Could not find pipe material with name " + asJson.getString("pipeMaterial"));
			}
		}
	}

}
