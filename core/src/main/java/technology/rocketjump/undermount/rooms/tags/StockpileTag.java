package technology.rocketjump.undermount.rooms.tags;

import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.entities.tags.TagProcessingUtils;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;

public class StockpileTag extends Tag {
	@Override
	public String getTagName() {
		return "STOCKPILE";
	}

	@Override
	public boolean isValid() {
		return args.size() == 0;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		room.createComponent(StockpileComponent.class, tagProcessingUtils.messageDispatcher);
	}

}
