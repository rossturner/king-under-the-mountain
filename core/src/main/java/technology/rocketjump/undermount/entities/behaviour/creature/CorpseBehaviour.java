package technology.rocketjump.undermount.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;
import technology.rocketjump.undermount.entities.components.EntityComponent;
import technology.rocketjump.undermount.entities.components.humanoid.HistoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.undermount.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.entities.model.physical.creature.Gender;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.undermount.persistence.model.InvalidSaveException;
import technology.rocketjump.undermount.persistence.model.SavedGameStateHolder;
import technology.rocketjump.undermount.rendering.utils.ColorMixer;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.i18n.I18nString;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorpseBehaviour implements BehaviourComponent, SelectableDescription {

	private SteeringComponent steeringComponent = new SteeringComponent(); // Is this needed?
	private double lastUpdateGameTime;
	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private Color originalSkinColor;
	private Color FULLY_DECAYED_COLOR;
	private double HOURS_TO_FULLY_DECAY;
	private double decayedAmount;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
		HOURS_TO_FULLY_DECAY = gameContext.getConstantsRepo().getWorldConstants().getCorpseDecayHours();
		FULLY_DECAYED_COLOR = gameContext.getConstantsRepo().getWorldConstants().getCorpseDecayColorInstance();
		steeringComponent.init(parentEntity, gameContext.getAreaMap(), parentEntity.getLocationComponent(), messageDispatcher);
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		CorpseBehaviour corpseBehaviour = new CorpseBehaviour();
		corpseBehaviour.originalSkinColor = this.originalSkinColor.cpy();
		return corpseBehaviour;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		if (decayedAmount < HOURS_TO_FULLY_DECAY) {
			double elapsedTime = gameContext.getGameClock().getCurrentGameTime() - lastUpdateGameTime;
			decayedAmount += elapsedTime;

			Color newSkinColor = ColorMixer.interpolate(0, (float) HOURS_TO_FULLY_DECAY, (float) decayedAmount, originalSkinColor, FULLY_DECAYED_COLOR);
			CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
			attributes.setSkinColor(newSkinColor);

			if (decayedAmount >= HOURS_TO_FULLY_DECAY) {
				// Switch to fully decayed
				setToFullyDecayed(attributes);
			}
		}
		lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	public void setToFullyDecayed(CreatureEntityAttributes attributes) {
		decayedAmount = HOURS_TO_FULLY_DECAY;
		attributes.setGender(Gender.NONE);
		ProfessionsComponent professionsComponent = parentEntity.getComponent(ProfessionsComponent.class);
		if (professionsComponent != null) {
			professionsComponent.clear();
		}
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
	}

	public void setOriginalSkinColor(Color originalSkinColor) {
		this.originalSkinColor = originalSkinColor;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// Do nothing
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return steeringComponent;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("originalSkin", HexColors.toHexString(originalSkinColor));
		asJson.put("decayed", decayedAmount);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		originalSkinColor = HexColors.get(asJson.getString("originalSkin"));
		decayedAmount = asJson.getDoubleValue("decayed");
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext) {
		HistoryComponent historyComponent = parentEntity.getComponent(HistoryComponent.class);
		if (historyComponent != null && historyComponent.getDeathReason() != null) {
			DeathReason reason = historyComponent.getDeathReason();

			Map<String, I18nString> replacements = new HashMap<>();
			replacements.put("reason", i18nTranslator.getDictionary().getWord(reason.getI18nKey()));
			I18nText deathDescriptionString = i18nTranslator.getTranslatedWordWithReplacements("NOTIFICATION.DEATH.SHORT_DESCRIPTION", replacements);
			return List.of(deathDescriptionString);
		} else {
			return List.of(i18nTranslator.getTranslatedString("CREATURE.STATUS.DEAD"));
		}
	}
}
