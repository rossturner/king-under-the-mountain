package technology.rocketjump.undermount.rooms.constructions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.google.common.collect.Sets;
import technology.rocketjump.undermount.assets.model.WallType;
import technology.rocketjump.undermount.entities.SequentialIdGenerator;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.mapping.tile.layout.WallConstructionLayout;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.persistence.JSONUtils;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;

import java.util.Set;

import static technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial.convert;
import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

public class WallConstruction extends Construction {

	private GridPoint2 location;
	private Set<GridPoint2> tileLocations;
	private WallType wallTypeToConstruct;

	private WallConstructionLayout layout = new WallConstructionLayout(0);

	public WallConstruction() {

	}

	public WallConstruction(GridPoint2 location, WallType wallTypeToConstruct, GameMaterial material) {
		this.constructionId = SequentialIdGenerator.nextId();
		this.location = location;
		this.tileLocations = Sets.newHashSet(location);
		this.wallTypeToConstruct = wallTypeToConstruct;
		this.primaryMaterialType = wallTypeToConstruct.getMaterialType();
		this.requirements = convert(wallTypeToConstruct.getRequirements().get(primaryMaterialType));
		if (material != null && !material.equals(NULL_MATERIAL)) {
			for (QuantifiedItemTypeWithMaterial requirement : requirements) {
				requirement.setMaterial(material);
			}
		}

	}

	@Override
	public Set<GridPoint2> getTileLocations() {
		return tileLocations;
	}

	@Override
	public GridPoint2 getPrimaryLocation() {
		return location;
	}

	@Override
	public ConstructionType getConstructionType() {
		return ConstructionType.WALL_CONSTRUCTION;
	}

	@Override
	public Entity getEntity() {
		return null;
	}

	public WallConstructionLayout getLayout() {
		return layout;
	}

	public void setLayout(WallConstructionLayout layout) {
		this.layout = layout;
	}

	public WallType getWallTypeToConstruct() {
		return wallTypeToConstruct;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.constructions.containsKey(getId())) {
			return;
		}
		super.writeTo(savedGameStateHolder);
		JSONObject asJson = savedGameStateHolder.constructionsJson.getJSONObject(savedGameStateHolder.constructionsJson.size() - 1);

		asJson.put("location", JSONUtils.toJSON(location));

		if (wallTypeToConstruct != null) {
			asJson.put("wallType", wallTypeToConstruct.getWallTypeName());
		}

		asJson.put("layout", layout.getId());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.location = JSONUtils.gridPoint2(asJson.getJSONObject("location"));
		this.tileLocations = Sets.newHashSet(this.location);

		String wallTypeName = asJson.getString("wallType");
		if (wallTypeName != null) {
			this.wallTypeToConstruct = relatedStores.wallTypeDictionary.getByWallTypeName(wallTypeName);
			if (wallTypeToConstruct == null) {
				throw new InvalidSaveException("Could not find wall type by name " + wallTypeName);
			}
		}

		this.layout = new WallConstructionLayout(asJson.getIntValue("layout"));
	}
}
