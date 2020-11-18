package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.GameViewMode;
import technology.rocketjump.undermount.ui.actions.ButtonAction;
import technology.rocketjump.undermount.ui.actions.SwitchGuiViewAction;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.LinkedList;
import java.util.List;

@Singleton
public class DefaultGuiView implements GuiView {

	private List<Actor> buttons = new LinkedList<>();

	@Inject
	public DefaultGuiView(IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher) {
		IconButton orders = iconButtonFactory.create("GUI.ORDERS_LABEL", "dig-dug", HexColors.get("#A1D479"), ButtonStyle.DEFAULT);
		orders.setAction(new SwitchGuiViewAction(GuiViewName.ORDER_SELECTION, messageDispatcher));
		buttons.add(orders);

		IconButton build = iconButtonFactory.create("GUI.BUILD_LABEL", "concrete-bag", HexColors.get("#539CD9"), ButtonStyle.DEFAULT);
		build.setAction(new SwitchGuiViewAction(GuiViewName.BUILD_MENU, messageDispatcher));
		buttons.add(build);

		IconButton zones = iconButtonFactory.create("GUI.ZONES_LABEL", "bed", HexColors.get("#C48C7A"), ButtonStyle.DEFAULT);
		zones.setAction(new SwitchGuiViewAction(GuiViewName.ROOM_SELECTION, messageDispatcher));
		buttons.add(zones);

		IconButton priority = iconButtonFactory.create("GUI.PRIORITY_LABEL", JobPriority.HIGHER.iconName, JobPriority.HIGHER.color, ButtonStyle.DEFAULT);
		priority.setAction(new ButtonAction() {
			@Override
			public void onClick() {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.PRIORITY_MENU);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.JOB_PRIORITY);
			}
		});
		buttons.add(priority);
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		for (Actor button : buttons) {
			containerTable.add(button).pad(5);
		}

	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

}
