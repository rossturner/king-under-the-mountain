package technology.rocketjump.undermount.entities.model.physical.creature.status.alcohol;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.humanoid.StatusComponent;
import technology.rocketjump.undermount.entities.model.physical.creature.status.StatusEffect;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class Drunk extends StatusEffect {

    public Drunk() {
        super(AlcoholDependent.class, 24.0 * 3.0, null);
    }

    @Override
    public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {

    }

    @Override
    public boolean checkForRemoval(GameContext gameContext) {
        return alreadyAlcoholDependent();
    }

    @Override
    public String getI18Key() {
        return "STATUS.DRUNK";
    }

    private boolean alreadyAlcoholDependent() {
        return parentEntity.getComponent(StatusComponent.class).contains(AlcoholDependent.class);
    }

}
