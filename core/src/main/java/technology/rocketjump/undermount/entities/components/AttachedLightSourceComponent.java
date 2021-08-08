package technology.rocketjump.undermount.entities.components;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Disposable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.lighting.LightProcessor;
import technology.rocketjump.undermount.rendering.lighting.PointLight;

import java.util.EnumMap;

public class AttachedLightSourceComponent implements InfrequentlyUpdatableComponent, Disposable {

	private PointLight light = new PointLight();
	private Entity parentEntity;
	private boolean requiresMeshUpdate = true;
	private boolean enabled = false; // For entity to toggle off in bright outdoor light
	private boolean useParentBodyColor;
	private GameContext gameContext;

	public AttachedLightSourceComponent() {

	}

	public AttachedLightSourceComponent(Entity parentEntity) {
		this.parentEntity = parentEntity;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.gameContext = gameContext;
		this.parentEntity = parentEntity;
	}

	@Override
	public void infrequentUpdate(double elapsedTime) {
		if (this.isUseParentBodyColor()) {
			EntityAttributes attributes = parentEntity.getPhysicalEntityComponent().getAttributes();
			if (attributes instanceof PlantEntityAttributes) {
				this.setColor(attributes.getColor(ColoringLayer.BRANCHES_COLOR));
			} else {
				Logger.warn("Not yet implemented: useParentBodyColor attached light source for type " + parentEntity.getType());
			}
		}

		if (parentEntity.getLocationComponent().getWorldPosition() != null) {
			MapTile currentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
			if (currentTile != null) {
				EnumMap<CompassDirection, MapVertex> vertexNeighboursOfCell = gameContext.getAreaMap().getVertexNeighboursOfCell(currentTile);

				float numVertices = 0f;
				float outdoorLight = 0f;
				for (MapVertex mapVertex : vertexNeighboursOfCell.values()) {
					numVertices += 1f;
					outdoorLight += mapVertex.getOutsideLightAmount();
				}

				outdoorLight = outdoorLight / numVertices;
				float currentSunlightAmount = gameContext.getMapEnvironment().getSunlightAmount();

				float nearbyLuminance = outdoorLight * currentSunlightAmount;

				if (nearbyLuminance > gameContext.getConstantsRepo().getWorldConstants().getAttachedLightSourceTogglePoint()) {
					this.setEnabled(false);
				} else {
					this.setEnabled(true);
				}

			}
		}
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		AttachedLightSourceComponent cloned = new AttachedLightSourceComponent(parentEntity);
		cloned.setColor(light.getColor());
		return cloned;
	}

	public void updatePosition() {
		if (parentEntity.getLocationComponent().getWorldOrParentPosition() != null) {
			// TODO adjust by light position offset based on orientation, currently glued to parent entity position + light radius in Y direction
			light.setWorldPosition(parentEntity.getLocationComponent().getWorldOrParentPosition().cpy().add(0, parentEntity.getLocationComponent().getRadius()));
			setUpdateRequired();
		}
	}

	public void setColor(Color color) {
		light.setColor(color);
	}

	public void setUpdateRequired() {
		requiresMeshUpdate = true;
	}

	public PointLight getLightForRendering(TiledMap tiledMap, LightProcessor lightProcessor) {
		if (requiresMeshUpdate) {
			lightProcessor.updateLightGeometry(light, tiledMap);
			requiresMeshUpdate = false;
		}
		return light;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setUseParentBodyColor(boolean useParentBodyColor) {
		this.useParentBodyColor = useParentBodyColor;
	}

	public boolean isUseParentBodyColor() {
		return useParentBodyColor;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONObject lightJson = new JSONObject(true);
		this.light.writeTo(lightJson, savedGameStateHolder);
		asJson.put("light", lightJson);

		if (enabled) {
			asJson.put("enabled", true);
		}
		if (useParentBodyColor) {
			asJson.put("useParentBodyColor", useParentBodyColor);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject lightJson = asJson.getJSONObject("light");
		this.light.readFrom(lightJson, savedGameStateHolder, relatedStores);
		// Always update mesh after load
		this.requiresMeshUpdate = true;
		this.enabled = asJson.getBooleanValue("enabled");
		this.useParentBodyColor = asJson.getBooleanValue("useParentBodyColor");
	}

	@Override
	public void dispose() {
		light.dispose();
	}
}
