package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.LiquidContainerComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.materials.model.GameMaterial;

public class LiquidContainerTag extends Tag {

	@Override
	public String getTagName() {
		return "LIQUID_CONTAINER";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return getCapacity() > 0;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent == null) {
			liquidContainerComponent = new LiquidContainerComponent();

			liquidContainerComponent.setMaxLiquidCapacity(Integer.valueOf(args.get(0)));

			if (args.size() > 1) {
				String specifiedMaterialArg = args.get(1);
				if (specifiedMaterialArg != null) {
					GameMaterial specifiedMaterial = tagProcessingUtils.materialDictionary.getByName(specifiedMaterialArg);
					if (specifiedMaterial == null) {
						Logger.error("Could not find material " + specifiedMaterialArg + " defined in " + getTagName() + " tag");
					} else {
						liquidContainerComponent.setTargetLiquidMaterial(specifiedMaterial);
					}
				}
			}

			if (args.size() > 2) {
				String arg3 = args.get(2);
				if (arg3.equalsIgnoreCase("ALWAYS_INACTIVE")) {
					liquidContainerComponent.setAlwaysInactive(true);
				} else {
					Logger.error("Unrecognised argument to " + this.getTagName() + " tag: " + args.get(2));
				}
			}

			liquidContainerComponent.init(entity, messageDispatcher, gameContext);

			entity.addComponent(liquidContainerComponent);
		} // else component already exists, don't re-add
	}

	public int getCapacity() {
		try {
			return Integer.valueOf(args.get(0));
		} catch (NumberFormatException e) {
			Logger.error("Can not parse " + args.get(0));
			return 0;
		}
	}

}
