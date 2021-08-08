package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

@Singleton
public class SelectStartLocationGuiView implements GuiView, GameContextAware {

	private IconButton confirmEmbarkButton;
	private Table containerTable;
	private GameContext gameContext;
	private boolean hidden;

	@Inject
	private SelectStartLocationGuiView(IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher) {
		confirmEmbarkButton = iconButtonFactory.create("GUI.EMBARK.START", "flying-flag", HexColors.POSITIVE_COLOR, ButtonStyle.DEFAULT);
		confirmEmbarkButton.setAction(() -> {
			if (gameContext.getAreaMap().getEmbarkPoint() != null) {
				this.hidden = true;
				messageDispatcher.dispatchMessage(MessageType.BEGIN_SPAWN_SETTLEMENT);
			}
		});
	}

	@Override
	public void populate(Table containerTable) {
		this.containerTable = containerTable;
	}

	@Override
	public void update() {
		if (containerTable != null && gameContext != null) {
			containerTable.clearChildren();
			if (gameContext.getAreaMap().getEmbarkPoint() != null && !hidden) {
				containerTable.add(confirmEmbarkButton).pad(5);
			}
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.SELECT_STARTING_LOCATION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.SELECT_STARTING_LOCATION;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.hidden = false;
	}
}
