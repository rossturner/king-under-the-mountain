package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.GameViewMode;
import technology.rocketjump.undermount.ui.actions.SwitchGuiViewAction;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static technology.rocketjump.undermount.ui.views.GuiViewName.DEFAULT_MENU;

@Singleton
public class PrioritiesGuiView implements GuiView {

	private final MessageDispatcher messageDispatcher;
	private List<Actor> buttons = new LinkedList<>();

	@Inject
	public PrioritiesGuiView(IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;


		IconButton back = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
		back.setAction(new SwitchGuiViewAction(DEFAULT_MENU, messageDispatcher));
		buttons.add(back);

		List<JobPriority> prioritiesLowToHigh = Arrays.asList(JobPriority.LOWEST, JobPriority.LOWER, JobPriority.NORMAL, JobPriority.HIGHER, JobPriority.HIGHEST);
		for (JobPriority jobPriority : prioritiesLowToHigh) {
			IconButton button = iconButtonFactory.create(jobPriority.i18nKey, jobPriority.iconName, jobPriority.color, ButtonStyle.DEFAULT);
			button.setAction(() -> {
				messageDispatcher.dispatchMessage(MessageType.REPLACE_JOB_PRIORITY, jobPriority);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.SET_JOB_PRIORITY);
			});
			buttons.add(button);
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.PRIORITY_MENU;
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

	@Override
	public void onClose() {
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
	}
}
