package technology.rocketjump.undermount.rooms.tags;

import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.tags.Tag;
import technology.rocketjump.undermount.entities.tags.TagProcessingUtils;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.StockpileGroup;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;

public class StockpileTag extends Tag {
	@Override
	public String getTagName() {
		return "STOCKPILE";
	}

	@Override
	public boolean isValid() {
		return args.size() == 1;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		StockpileComponent stockpileComponent = room.createComponent(StockpileComponent.class, tagProcessingUtils.messageDispatcher);
		StockpileGroup group = tagProcessingUtils.stockpileGroupDictionary.getByName(args.get(0));
		if (group == null) {
			Logger.error("Unrecognised stockpile group defined in tag: " + args.get(0));
		} else {
			stockpileComponent.setGroup(group);
		}
	}
}
