package technology.rocketjump.undermount.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.components.LiquidAmountChangedMessage;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static technology.rocketjump.undermount.materials.model.GameMaterial.NULL_MATERIAL;

/**
 * This class is responsible for keeping track of all liquids in LiquidContainerComponents
 */
@Singleton
public class LiquidTracker implements GameContextAware, Telegraph {

	private final Map<GameMaterial, Float> liquidMaterialAmounts = new LinkedHashMap<>();

	@Inject
	public LiquidTracker(MessageDispatcher messageDispatcher) {
		messageDispatcher.addListener(this, MessageType.LIQUID_AMOUNT_CHANGED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.LIQUID_AMOUNT_CHANGED: {
				return handle((LiquidAmountChangedMessage)msg.extraInfo);
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public float getCurrentLiquidAmount(GameMaterial liquidMaterial) {
		return liquidMaterialAmounts.getOrDefault(liquidMaterial, 0f);
	}

	public Set<GameMaterial> getAllMaterials() {
		return liquidMaterialAmounts.keySet();
	}

	private boolean handle(LiquidAmountChangedMessage amountChangedMessage) {
		if (amountChangedMessage.liquidMaterial != null && !amountChangedMessage.liquidMaterial.equals(NULL_MATERIAL)) {
			Float currentTotal = liquidMaterialAmounts.getOrDefault(amountChangedMessage.liquidMaterial, 0f);
			currentTotal -= amountChangedMessage.oldQuantity;
			currentTotal += amountChangedMessage.newQuantity;
			liquidMaterialAmounts.put(amountChangedMessage.liquidMaterial, currentTotal);
		}
		return true;
	}

	@Override
	public void clearContextRelatedState() {
		liquidMaterialAmounts.clear();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		for (Entity entity : gameContext.getEntities().values()) {
			LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null) {
				handle(new LiquidAmountChangedMessage(entity, liquidContainerComponent.getTargetLiquidMaterial(), 0, liquidContainerComponent.getLiquidQuantity()));
			}
		}
	}
}
