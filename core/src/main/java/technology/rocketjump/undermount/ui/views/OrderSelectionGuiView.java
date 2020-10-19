package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.actions.SetInteractionMode;
import technology.rocketjump.undermount.ui.actions.SwitchGuiViewAction;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.LinkedList;
import java.util.List;

@Singleton
public class OrderSelectionGuiView implements GuiView {

	private List<IconButton> iconButtons = new LinkedList<>();

	@Inject
	public OrderSelectionGuiView(IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher) {

		IconButton back = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
		back.setAction(new SwitchGuiViewAction(GuiViewName.DEFAULT_MENU, messageDispatcher));
		iconButtons.add(back);

		IconButton mine = iconButtonFactory.create("GUI.ORDERS.MINE", "mining", HexColors.get("#97CFC7"), ButtonStyle.DEFAULT);
		mine.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_MINING, messageDispatcher));
		iconButtons.add(mine);

		IconButton chopTrees = iconButtonFactory.create("GUI.ORDERS.CHOP_WOOD", "logging", HexColors.get("#41AB44"), ButtonStyle.DEFAULT);
		chopTrees.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_CHOP_WOOD, messageDispatcher));
		iconButtons.add(chopTrees);

		IconButton harvest = iconButtonFactory.create("GUI.ORDERS.HARVEST_PLANTS", "sickle", HexColors.get("#e0dc6a"), ButtonStyle.DEFAULT);
		harvest.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_HARVEST_PLANTS, messageDispatcher));
		iconButtons.add(harvest);

		IconButton clearGround = iconButtonFactory.create("GUI.ORDERS.CLEAR_GROUND", "spade", HexColors.get("#B3733B"), ButtonStyle.DEFAULT);
		clearGround.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_CLEAR_GROUND, messageDispatcher));
		iconButtons.add(clearGround);

		IconButton removeDesignations = iconButtonFactory.create("GUI.REMOVE_LABEL", "cancel", HexColors.get("#D4534C"), ButtonStyle.DEFAULT);
		removeDesignations.setAction(new SetInteractionMode(GameInteractionMode.REMOVE_DESIGNATIONS, messageDispatcher));
		iconButtons.add(removeDesignations);

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ORDER_SELECTION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
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
