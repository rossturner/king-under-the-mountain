package technology.rocketjump.undermount.entities.components;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Disposable;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.model.TiledMap;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.lighting.LightProcessor;
import technology.rocketjump.undermount.rendering.lighting.PointLight;

public class AttachedLightSourceComponent implements ParentDependentEntityComponent, Disposable {

	private PointLight light = new PointLight();
	private Entity parentEntity;
	private boolean requiresMeshUpdate = true;
	private boolean enabled = false; // For entity to toggle off in bright outdoor light
	private boolean useParentBodyColor;

	public AttachedLightSourceComponent() {

	}

	public AttachedLightSourceComponent(Entity parentEntity) {
		this.parentEntity = parentEntity;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
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
