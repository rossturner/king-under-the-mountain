package technology.rocketjump.undermount.entities.model.physical.plant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.environment.model.Season;
import technology.rocketjump.undermount.materials.model.GameMaterial;

import java.util.ArrayList;
import java.util.List;

public class PlantSpeciesSeed {

	private String itemTypeName;
	private String materialName;
	private List<Season> plantingSeasons = new ArrayList<>();

	@JsonIgnore
	private ItemType seedItemType;
	@JsonIgnore
	private GameMaterial seedMaterial;

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public List<Season> getPlantingSeasons() {
		return plantingSeasons;
	}

	public void setPlantingSeasons(List<Season> plantingSeasons) {
		this.plantingSeasons = plantingSeasons;
	}

	public ItemType getSeedItemType() {
		return seedItemType;
	}

	public void setSeedItemType(ItemType seedItemType) {
		this.seedItemType = seedItemType;
	}

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public GameMaterial getSeedMaterial() {
		return seedMaterial;
	}

	public void setSeedMaterial(GameMaterial seedMaterial) {
		this.seedMaterial = seedMaterial;
	}
}
