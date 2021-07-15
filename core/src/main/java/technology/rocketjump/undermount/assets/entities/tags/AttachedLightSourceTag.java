package technology.rocketjump.undermount.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.entities.tags.TagProcessingUtils;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.rendering.utils.ColorMixer;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public class AttachedLightSourceTag extends Tag {

	@Override
	public String getTagName() {
		return "ATTACHES_LIGHT_SOURCE";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return args.size() >= 1; // FIXME #109 better validation that args are valid
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		AttachedLightSourceComponent attachedLightSourceComponent = entity.getComponent(AttachedLightSourceComponent.class);
		if (attachedLightSourceComponent == null) {
			attachedLightSourceComponent = new AttachedLightSourceComponent();
			attachedLightSourceComponent.init(entity, messageDispatcher, gameContext);
			attachedLightSourceComponent.updatePosition();
			if (entity.getType().equals(EntityType.ONGOING_EFFECT)) {
				attachedLightSourceComponent.setEnabled(true);
			}
			entity.addComponent(attachedLightSourceComponent);
		}

		Array<Color> colors = parseArgsToColors();

		Color lightColor = ColorMixer.randomBlend(new RandomXS128(entity.getId()), colors);
		attachedLightSourceComponent.setColor(lightColor);
		if (args.get(0).equals("PARENT_BODY_COLOR")) {
			attachedLightSourceComponent.setUseParentBodyColor(true);
		}
	}

	private Array<Color> parseArgsToColors() {
		Array<Color> colors = new Array<>();
		for (String arg : args) {
			if (arg.equalsIgnoreCase("PARENT_BODY_COLOR")) {
				colors.add(Color.WHITE);
			} else {
				colors.add(HexColors.get(arg));
			}
		}
		return colors;
	}
}
