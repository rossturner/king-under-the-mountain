package technology.rocketjump.undermount.settlement.production;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

public class ProductionQuota implements ChildPersistable {

	private Integer fixedAmount;
	private Float perSettler;

	public boolean isFixedAmount() {
		return fixedAmount != null;
	}

	public Integer getFixedAmount() {
		return fixedAmount;
	}

	public void setFixedAmount(Integer fixedAmount) {
		this.fixedAmount = fixedAmount;
	}

	public Float getPerSettler() {
		return perSettler;
	}

	public void setPerSettler(Float perSettler) {
		this.perSettler = perSettler;
	}

	public int getRequiredAmount(int numSettlers) {
		if (isFixedAmount()) {
			return fixedAmount;
		} else {
			return (int) Math.ceil(perSettler * (float)numSettlers);
		}
	}

	@Override
	public String toString() {
		if (isFixedAmount()) {
			return "fixedAmount=" + fixedAmount;
		} else {

			return "perSettler=" + perSettler;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (fixedAmount != null) {
			asJson.put("fixedAmount", fixedAmount);
		}
		if (perSettler != null) {
			asJson.put("perSettler", perSettler);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.fixedAmount = asJson.getInteger("fixedAmount");
		this.perSettler = asJson.getFloat("perSettler");
	}
}
