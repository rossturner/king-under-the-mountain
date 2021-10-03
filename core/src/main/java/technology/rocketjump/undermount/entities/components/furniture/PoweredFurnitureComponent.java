package technology.rocketjump.undermount.entities.components.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.misc.Destructible;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureLayout.SpecialTile.SpecialTileRequirment.IS_RIVER;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;

public class PoweredFurnitureComponent implements ParentDependentEntityComponent, Destructible, SelectableDescription {

	private Entity parentEntity;
	private GameContext gameContext;
	private int powerAmount;
	private float animationSpeed; // also used as powered crafting station job time multiplier
	private boolean animatedReversed;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.gameContext = gameContext;

		initialiseAnimationDirection(parentEntity, gameContext);
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		PoweredFurnitureComponent clone = new PoweredFurnitureComponent();
		clone.powerAmount = this.powerAmount;
		clone.animationSpeed = this.animationSpeed;
		return clone;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		setPowerAmount(0);
		updatePowerGridAtParentLocation();
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		parentTile.getOrCreateUnderTile().setPowerSource(false);
		parentTile.getOrCreateUnderTile().setPowerConsumer(false);
	}

	public void update(float deltaTime, GameContext gameContext) {
		UnderTile underTile = getParentUnderTile(gameContext);
		if (powerAmount > 0) {
			underTile.setPowerSource(true);
		} else if (powerAmount < 0) {
			underTile.setPowerConsumer(true);
		}
		boolean powered = isPowered(underTile);

		if (powered) {
			float animationProgress = parentEntity.getPhysicalEntityComponent().getAnimationProgress();
			float animationDelta = deltaTime * animationSpeed;
			if (animatedReversed) {
				animationProgress -= animationDelta;
				while (animationProgress < 0f) {
					animationProgress += 1f;
				}
			} else {
				animationProgress += animationDelta;
				while (animationProgress > 1f) {
					animationProgress -= 1f;
				}
			}
			parentEntity.getPhysicalEntityComponent().setAnimationProgress(animationProgress);
		}
	}

	public boolean isPowered(UnderTile underTile) {
		boolean powered = false;
		if (underTile.getPowerGrid() != null) {
			powered = underTile.getPowerGrid().getTotalPowerAvailable() > 0;
		}
		return powered;
	}

	public UnderTile getParentUnderTile(GameContext gameContext) {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		UnderTile underTile = parentTile.getOrCreateUnderTile();
		return underTile;
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		if (((FurnitureEntityAttributes)parentEntity.getPhysicalEntityComponent().getAttributes()).isDestroyed()) {
			return emptyList();
		}
		boolean powered = isPowered(getParentUnderTile(gameContext));
		List<I18nText> result = new ArrayList<>();
		if (powerAmount < 0) {
			// wants to consume power
			if (powered) {
				// Powered - consuming -powerAmount units
				I18nWord word = i18nTranslator.getDictionary().getWord("MECHANISM.FURNITURE.CONSUMING_POWER");
				result.add(i18nTranslator.applyReplacements(word, Map.of("power", new I18nWord(String.valueOf(-powerAmount))), Gender.ANY));
			} else {
				// needs power grid
				result.add(i18nTranslator.getTranslatedString("MECHANISM.FURNITURE.REQUIRES_POWER"));
			}
		} else {
			// producing power
			if (powered) {
				// producing X power
				I18nWord word = i18nTranslator.getDictionary().getWord("MECHANISM.FURNITURE.PRODUCING_POWER");
				result.add(i18nTranslator.applyReplacements(word, Map.of("power", new I18nWord(String.valueOf(powerAmount))), Gender.ANY));
			} else {
				// needs power grid
				result.add(i18nTranslator.getTranslatedString("MECHANISM.FURNITURE.REQUIRES_POWER_GRID"));
			}
		}
		return result;
	}

	public void updatePowerGridAtParentLocation() {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null && parentTile.getUnderTile() != null && parentTile.getUnderTile().getPowerGrid() != null) {
			parentTile.getUnderTile().getPowerGrid().update(gameContext);
		}
	}

	private void initialiseAnimationDirection(Entity parentEntity, GameContext gameContext) {
		if (parentEntity.getType().equals(EntityType.FURNITURE)) {
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
			FurnitureLayout currentLayout = attributes.getCurrentLayout();
			if (!currentLayout.getSpecialTiles().isEmpty()) {
				FurnitureLayout.SpecialTile specialTile = currentLayout.getSpecialTiles().get(0);
				if (specialTile.getRequirement().equals(IS_RIVER)) {
					GridPoint2 specialTileLocation = toGridPoint(parentEntity.getLocationComponent().getWorldPosition()).add(specialTile.getLocation());
					MapVertex[] vertices = gameContext.getAreaMap().getVertices(specialTileLocation.x, specialTileLocation.y);
					Vector2 averageWaterFlow = new Vector2();
					for (MapVertex vertex : vertices) {
						averageWaterFlow.add(vertex.getWaterFlowDirection());
					}
					averageWaterFlow.scl(1f / (float)vertices.length);

					if (Math.abs(averageWaterFlow.x) > Math.abs(averageWaterFlow.y)) {
						// x component is greater
						if (averageWaterFlow.x < 0) {
							// flowing west to east
							animatedReversed = true;
						}
					} else {
						if (averageWaterFlow.y < 0) {
							// flowing north to south
							animatedReversed = true;
						}
					}
				}
			}
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("powerAmount", powerAmount);
		asJson.put("animationSpeed", animationSpeed);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.powerAmount = asJson.getIntValue("powerAmount");
		this.animationSpeed = asJson.getFloatValue("animationSpeed");
	}

	public void setPowerAmount(int powerAmount) {
		this.powerAmount = powerAmount;
	}

	public int getPowerAmount() {
		return powerAmount;
	}

	public void setAnimationSpeed(float animationSpeed) {
		this.animationSpeed = animationSpeed;
	}

	public float getAnimationSpeed() {
		return animationSpeed;
	}
}
