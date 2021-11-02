package technology.rocketjump.undermount.entities.model.physical.creature.status.alcohol;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.ai.memory.Memory;
import technology.rocketjump.undermount.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.status.StatusEffect;
import technology.rocketjump.undermount.gamecontext.GameContext;

import java.util.Deque;

import static technology.rocketjump.undermount.entities.ai.memory.MemoryType.CONSUMED_ALCOHOLIC_DRINK;

public class AlcoholDependent extends StatusEffect {

    public AlcoholDependent() {
        super(AlcoholWithdrawl.class, 24.0 * 3.0, null);
    }

    @Override
    public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
        MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);
        Deque<Memory> shortTermMemories = memoryComponent.getShortTermMemories(gameContext.getGameClock());

        shortTermMemories.stream().filter(mem -> mem.getType().equals(CONSUMED_ALCOHOLIC_DRINK)).findAny().ifPresent(memoryOfDrink -> {
            // Reset time applied if a drink has been consumed recently
            this.timeApplied = 0;
        });
    }

    @Override
    public boolean checkForRemoval(GameContext gameContext) {
        // Only removed by time expiry to next stage
        return false;
    }

    @Override
    public String getI18Key() {
        return null;
    }
}
