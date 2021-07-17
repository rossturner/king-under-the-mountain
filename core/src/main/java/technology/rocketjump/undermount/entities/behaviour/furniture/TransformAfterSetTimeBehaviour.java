package technology.rocketjump.undermount.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.ImmutableMap;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.furniture.FurnitureParticleEffectsComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.TransformFurnitureMessage;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.undermount.ui.i18n.I18nTranslator.oneDecimalFormat;

public class TransformAfterSetTimeBehaviour extends FurnitureBehaviour implements SelectableDescription {

	private static final double TOTAL_TIME = 50.0;
	private double timeRemaining = TOTAL_TIME;
	private double lastUpdateGameTime = 0;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		if (relatedFurnitureTypes.size() != 1) {
			Logger.error("Expecting 1 related furniture type for " + this.getClass().getSimpleName());
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		double elapsed = gameContext.getGameClock().getCurrentGameTime() - lastUpdateGameTime;
		timeRemaining -= elapsed;
		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();

		FurnitureParticleEffectsComponent particleEffectsComponent = parentEntity.getComponent(FurnitureParticleEffectsComponent.class);
		if (particleEffectsComponent != null) {
			particleEffectsComponent.triggerProcessingEffects(
					Optional.ofNullable(new JobTarget(parentEntity)));
		}

		if (timeRemaining <= 0) {
			if (particleEffectsComponent != null) {
				particleEffectsComponent.releaseParticles();
			}

			messageDispatcher.dispatchMessage(MessageType.TRANSFORM_FURNITURE_TYPE, new TransformFurnitureMessage(parentEntity, relatedFurnitureTypes.get(0)));
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
		asJson.put("timeRemaining", timeRemaining);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
		this.timeRemaining = asJson.getDoubleValue("timeRemaining");
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		List<I18nText> descriptions = new ArrayList<>(1);
		double progress = (TOTAL_TIME - timeRemaining) / TOTAL_TIME;
		descriptions.add(i18nTranslator.getTranslatedWordWithReplacements("FURNITURE.DESCRIPTION.GENERIC_PROGRESS",
				ImmutableMap.of("progress", new I18nText(oneDecimalFormat.format(100f * progress)))));
		return descriptions;
	}
}
