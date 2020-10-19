package technology.rocketjump.undermount.entities.model.physical.humanoid.status.alcohol;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.model.physical.humanoid.status.StatusEffect;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.StatusMessage;

import java.util.Deque;

import static technology.rocketjump.undermount.entities.ai.memory.MemoryType.CONSUMED_ALCOHOLIC_DRINK;

public class AlcoholWithdrawl extends StatusEffect {

    public AlcoholWithdrawl() {
        super(null, 24 * 3, null);
    }

    @Override
    public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
        if (recentDrink(gameContext)) {
            messageDispatcher.dispatchMessage(MessageType.REMOVE_STATUS, new StatusMessage(parentEntity, this.getClass(), null));
            messageDispatcher.dispatchMessage(MessageType.APPLY_STATUS, new StatusMessage(parentEntity, AlcoholDependent.class, null));
        } else {
            parentEntity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.ALCOHOL_WITHDRAWL);
        }
    }

    private boolean recentDrink(GameContext gameContext) {
        MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);
        Deque<Memory> shortTermMemories = memoryComponent.getShortTermMemories(gameContext.getGameClock());
        return shortTermMemories.stream().anyMatch(mem -> mem.getType().equals(CONSUMED_ALCOHOLIC_DRINK));
    }

    @Override
    public boolean checkForRemoval(GameContext gameContext) {
        // As this does not have a nextStage property, need to remove this when applied for X hours
        return timeApplied > this.hoursUntilNextStage;
    }
}
