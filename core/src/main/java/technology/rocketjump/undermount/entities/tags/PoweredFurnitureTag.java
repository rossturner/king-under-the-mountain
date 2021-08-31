package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.undermount.entities.components.furniture.PoweredFurnitureComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class PoweredFurnitureTag extends Tag {

	@Override
	public String getTagName() {
		return "POWERED_FURNITURE";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return Integer.valueOf(args.get(0)) != null && Float.valueOf(args.get(1)) != null;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getBehaviourComponent() == null) {
			// Don't apply to furniture which already doesn't have a BehaviourComponent e.g. when placing from UI
			return;
		}

		int powerAmount = Integer.parseInt(args.get(0));
		float animationFps = Float.parseFloat(args.get(1));

		PoweredFurnitureComponent poweredFurnitureComponent = new PoweredFurnitureComponent();
		poweredFurnitureComponent.setPowerAmount(powerAmount);
		poweredFurnitureComponent.setAnimationSpeed(animationFps);
		poweredFurnitureComponent.init(entity, messageDispatcher, gameContext);
		entity.addComponent(poweredFurnitureComponent);
	}

}
