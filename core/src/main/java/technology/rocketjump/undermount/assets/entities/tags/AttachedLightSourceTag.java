package technology.rocketjump.undermount.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.undermount.entities.model.Entity;
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
	public boolean isValid() {
		return args.size() == 1; // FIXME #109 better validation that args are valid
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		AttachedLightSourceComponent attachedLightSourceComponent = entity.getComponent(AttachedLightSourceComponent.class);
		if (attachedLightSourceComponent == null) {
			attachedLightSourceComponent = new AttachedLightSourceComponent();
			attachedLightSourceComponent.init(entity, messageDispatcher, gameContext);
			attachedLightSourceComponent.updatePosition();
			entity.addComponent(attachedLightSourceComponent);
		}

		String colourType = args.get(0);

		Color lightColor = ColorMixer.randomBlend(new RandomXS128(entity.getId()), getColors(colourType));
		attachedLightSourceComponent.setColor(lightColor);
		if (colourType.equals("PARENT_BODY_COLOR")) {
			attachedLightSourceComponent.setUseParentBodyColor(true);
		}
	}

	private static Array<Color> getColors(String colourType) {
		Array<Color> colors = new Array<>();
		if (colourType.equals("ORANGE")) {
			colors.add(HexColors.get("#fff95b"));
			colors.add(HexColors.get("#ffe487"));
			colors.add(HexColors.get("#ffd39f"));
			colors.add(HexColors.get("#ffb99e"));
		} else if (colourType.equals("GREEN")) {
			colors.add(HexColors.get("#85f3ce"));
			colors.add(HexColors.get("#a7f3c3"));
			colors.add(HexColors.get("#d9ffda"));
			colors.add(HexColors.get("#cdffb7"));
		} else if (colourType.equals("YELLOW")) {
			colors.add(HexColors.get("#f6f76f"));
			colors.add(HexColors.get("#fdfda0"));
			colors.add(HexColors.get("#fafaec"));
			colors.add(HexColors.get("#f4f5c0"));
		} else if (colourType.equals("PARENT_BODY_COLOR")) {
			colors.add(Color.WHITE);
		} else {
			Logger.error("Not yet implemented attached light source color: " + colourType);
			colors.add(Color.WHITE);
		}
		return colors;
	}
}
