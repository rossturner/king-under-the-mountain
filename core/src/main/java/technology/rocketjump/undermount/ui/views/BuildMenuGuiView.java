package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.GameViewMode;
import technology.rocketjump.undermount.ui.actions.SetInteractionMode;
import technology.rocketjump.undermount.ui.actions.SwitchGuiViewAction;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.LinkedList;
import java.util.List;

import static technology.rocketjump.undermount.ui.views.GuiViewName.*;

@Singleton
public class BuildMenuGuiView implements GuiView {

	private List<IconButton> iconButtons = new LinkedList<>();

	@Inject
	public BuildMenuGuiView(IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher) {

		IconButton back = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
		back.setAction(new SwitchGuiViewAction(DEFAULT_MENU, messageDispatcher));
		iconButtons.add(back);

		IconButton walls = iconButtonFactory.create("GUI.BUILD.WALLS", "stone-wall", HexColors.get("#cdcda7"), ButtonStyle.DEFAULT);
		walls.setAction(new SwitchGuiViewAction(GuiViewName.BUILD_WALLS, messageDispatcher));
		iconButtons.add(walls);

		IconButton roofing = iconButtonFactory.create("GUI.BUILD.ROOFING", "triple-gate", HexColors.get("#7777ed"), ButtonStyle.DEFAULT);
		roofing.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.ROOFING_INFO);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.BUILD_ROOFING);
		});
		iconButtons.add(roofing);

		IconButton doors = iconButtonFactory.create("GUI.BUILD.DOORS", "closed-doors", HexColors.get("#dca27b"), ButtonStyle.DEFAULT);
		doors.setAction(new SwitchGuiViewAction(GuiViewName.BUILD_DOORS, messageDispatcher));
		iconButtons.add(doors);

		IconButton bridge = iconButtonFactory.create("GUI.BUILD.BRIDGE", "stone-bridge", HexColors.get("#8fd0c1"), ButtonStyle.DEFAULT);
		bridge.setAction(new SwitchGuiViewAction(GuiViewName.BUILD_BRIDGE, messageDispatcher));
		iconButtons.add(bridge);

		IconButton furniture = iconButtonFactory.create("GUI.BUILD_FURNITURE", "hammer-nails", HexColors.get("#1a7ce1"), ButtonStyle.DEFAULT);
		furniture.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_ROOM_TYPE_SELECTED, null);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, ROOM_FURNITURE_SELECTION);
		});
		iconButtons.add(furniture);

		IconButton removeConstructions = iconButtonFactory.create("GUI.CANCEL_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.DEFAULT);
		removeConstructions.setAction(new SetInteractionMode(GameInteractionMode.REMOVE_CONSTRUCTIONS, messageDispatcher));
		iconButtons.add(removeConstructions);

		IconButton removeDesignations = iconButtonFactory.create("GUI.DECONSTRUCT_LABEL", "demolish", HexColors.get("#d1752e"), ButtonStyle.DEFAULT);
		removeDesignations.setAction(new SetInteractionMode(GameInteractionMode.DECONSTRUCT, messageDispatcher));
		iconButtons.add(removeDesignations);
	}

	@Override
	public GuiViewName getName() {
		return BUILD_MENU;
	}

	@Override
	public GuiViewName getParentViewName() {
		return DEFAULT_MENU;
	}

	@Override
	public void populate(Table containerTable) {

		for (IconButton iconButton : iconButtons) {
			containerTable.add(iconButton).pad(5);
		}
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

}
