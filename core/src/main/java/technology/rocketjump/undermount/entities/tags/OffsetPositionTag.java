package technology.rocketjump.undermount.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.math.NumberUtils;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.gamecontext.GameContext;

public class OffsetPositionTag extends Tag {

	@Override
	public String getTagName() {
		return "OFFSET_POSITION";
	}

	@Override
	public boolean isValid() {
		return args.size() == 2 && NumberUtils.isNumber(args.get(0)) && NumberUtils.isNumber(args.get(1));
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		Vector2 offset = new Vector2(Float.valueOf(args.get(0)), Float.valueOf(args.get(1)));

		// Reset offset in current tile by 0.5f + offset each time, so multiple applications do not accumulate
		Vector2 worldPosition = entity.getLocationComponent().getWorldPosition().cpy();
		worldPosition.x = (float) Math.floor(worldPosition.x);
		worldPosition.x = worldPosition.x + 0.5f + offset.x;
		worldPosition.y = (float) Math.floor(worldPosition.y);
		worldPosition.y = worldPosition.y + 0.5f + offset.y;

		entity.getLocationComponent().setWorldPosition(worldPosition, false);
	}

}
