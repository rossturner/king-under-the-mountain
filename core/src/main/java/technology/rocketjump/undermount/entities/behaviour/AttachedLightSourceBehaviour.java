package technology.rocketjump.undermount.entities.behaviour;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.EntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;

import java.util.EnumMap;

public class AttachedLightSourceBehaviour {

	public static void infrequentUpdate(GameContext gameContext, Entity parentEntity) {
		AttachedLightSourceComponent attachedLightSourceComponent = parentEntity.getComponent(AttachedLightSourceComponent.class);
		if (attachedLightSourceComponent != null) {
			if (attachedLightSourceComponent.isUseParentBodyColor()) {
				EntityAttributes attributes = parentEntity.getPhysicalEntityComponent().getAttributes();
				if (attributes instanceof PlantEntityAttributes) {
					attachedLightSourceComponent.setColor(attributes.getColor(ColoringLayer.BRANCHES_COLOR));
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
					float currentSunlightAmount = gameContext.getAreaMap().getEnvironment().getSunlightAmount();

					float nearbyLuminance = outdoorLight * currentSunlightAmount;

					if (nearbyLuminance > gameContext.getConstantsRepo().getWorldConstants().getAttachedLightSourceTogglePoint()) {
						attachedLightSourceComponent.setEnabled(false);
					} else {
						attachedLightSourceComponent.setEnabled(true);
					}

				}
			}
		}
	}
}
