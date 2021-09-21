package technology.rocketjump.undermount.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.gamecontext.GameState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.InfoWindow;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.ui.cursor.CursorManager;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.views.*;
import technology.rocketjump.undermount.ui.views.debug.DebugGuiView;
import technology.rocketjump.undermount.ui.widgets.GameDialog;
import technology.rocketjump.undermount.ui.widgets.tooltips.Tooltip;

import java.util.Arrays;
import java.util.List;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.UI_SCALE;
import static technology.rocketjump.undermount.rendering.camera.DisplaySettings.DEFAULT_UI_SCALE;

@Singleton
public class GuiContainer implements Telegraph, GameContextAware {

	private final InfoWindow infoWindow;
	private final Table containerTable;
	private final Table upperRightContainerTable;
	private final Table upperLeftContainerTable;
	private final Table lowerRightContainerTable;
	private final Table timeAndDateContainerTable;
	private final Table hintContainerTable;
	private final Table debugContainerTable;
	private final Table notificationTable;
	private final Table minimapContainerTable;
	private final MessageDispatcher messageDispatcher;
	private final CursorManager cursorManager;
	private final GameInteractionStateContainer interactionStateContainer;
	private final TimeDateGuiView timeDateGuiView;
	private final HintGuiView hintGuiView;
	private final DebugGuiView debugGuiView;
	private final NotificationGuiView notificationGuiView;
	private final MinimapGuiView minimapGuiView;
	private Stage primaryStage;
	private Stage timeAndDateStage;
	private StageAreaOnlyInputHandler primaryStageInputHandler;
	private StageAreaOnlyInputHandler timeAndDateStageInputHandler;

	private final GuiViewRepository guiViewRepository;
	private GuiViewName currentViewName;
	private GuiView currentView;
	private float timeSinceLastUpdate = 0;

	@Inject
	public GuiContainer(MessageDispatcher messageDispatcher, CursorManager cursorManager, GameInteractionStateContainer interactionStateContainer,
						GuiSkinRepository guiSkinRepository, GuiViewRepository guiViewRepository, TimeDateGuiView timeDateGuiView,
						UserPreferences userPreferences, InfoWindow infoWindow, HintGuiView hintGuiView,
						DebugGuiView debugGuiView, NotificationGuiView notificationGuiView,
						MinimapGuiView minimapGuiView) {
		this.infoWindow = infoWindow;
		this.hintGuiView = hintGuiView;
		this.debugGuiView = debugGuiView;
		this.notificationGuiView = notificationGuiView;
		Skin uiSkin = guiSkinRepository.getDefault();
		this.timeDateGuiView = timeDateGuiView;
		this.minimapGuiView = minimapGuiView;
		this.cursorManager = cursorManager;
		this.interactionStateContainer = interactionStateContainer;

		ScreenViewport viewport = new ScreenViewport();

		float savedScale = Float.valueOf(userPreferences.getPreference(UI_SCALE, DEFAULT_UI_SCALE));
		viewport.setUnitsPerPixel(1 / savedScale);
		primaryStage = new Stage(viewport);
		primaryStageInputHandler = new StageAreaOnlyInputHandler(primaryStage, interactionStateContainer);

		containerTable = new Table(uiSkin);
		containerTable.setFillParent(true);
		containerTable.pad(10f); // Table edge padding
		containerTable.left().bottom();

		primaryStage.addActor(containerTable);

		this.messageDispatcher = messageDispatcher;
		messageDispatcher.addListener(this, MessageType.GUI_SWITCH_VIEW);
		messageDispatcher.addListener(this, MessageType.GUI_SCALE_CHANGED);
		messageDispatcher.addListener(this, MessageType.GUI_SWITCH_INTERACTION_MODE);
		messageDispatcher.addListener(this, MessageType.GUI_SWITCH_VIEW_MODE);
		messageDispatcher.addListener(this, MessageType.GUI_CANCEL_CURRENT_VIEW);
		messageDispatcher.addListener(this, MessageType.GUI_CANCEL_CURRENT_VIEW_OR_GO_TO_MAIN_MENU);

		this.guiViewRepository = guiViewRepository;
		switchView(GuiViewName.DEFAULT_MENU);

		timeAndDateStage = new Stage(viewport);

		timeAndDateContainerTable = new Table(uiSkin);
		timeAndDateContainerTable.right().top();

		hintContainerTable = new Table(uiSkin);
		hintContainerTable.left().top();
		debugContainerTable = new Table(uiSkin);
		debugContainerTable.left().top();

		notificationTable = new Table(uiSkin);
		minimapContainerTable = new Table(uiSkin);

		upperRightContainerTable = new Table(uiSkin);
		upperRightContainerTable.right().top();
		upperRightContainerTable.add(timeAndDateContainerTable).top().right().row();
		upperRightContainerTable.add(notificationTable).right();
		upperRightContainerTable.setFillParent(true);

		upperLeftContainerTable = new Table(uiSkin);
		upperLeftContainerTable.left().top();
		upperLeftContainerTable.add(hintContainerTable).top().left().pad(10).row();
		upperLeftContainerTable.add(debugContainerTable).top().left().pad(10).row();
		upperLeftContainerTable.setFillParent(true);

		lowerRightContainerTable = new Table(uiSkin);
		lowerRightContainerTable.right().bottom();
		lowerRightContainerTable.add(minimapContainerTable).bottom().right().row();
		lowerRightContainerTable.setFillParent(true);

		timeAndDateStage.addActor(upperRightContainerTable);
		primaryStage.addActor(upperLeftContainerTable);
		timeAndDateStage.addActor(lowerRightContainerTable);
		timeDateGuiView.populate(timeAndDateContainerTable);
		timeAndDateContainerTable.row();
		notificationGuiView.populate(timeAndDateContainerTable);
		minimapGuiView.populate(minimapContainerTable);
		hintGuiView.populate(hintContainerTable);
		debugGuiView.populate(debugContainerTable);
		debugGuiView.update();

		timeAndDateStageInputHandler = new StageAreaOnlyInputHandler(timeAndDateStage, interactionStateContainer);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GUI_SWITCH_VIEW: {
				GuiViewName targetView = (GuiViewName)msg.extraInfo;
				switchView(targetView);
				return true;
			}
			case MessageType.GUI_SCALE_CHANGED: {
				Float scale = (Float)msg.extraInfo;
				ScreenViewport viewport = new ScreenViewport();
				viewport.setUnitsPerPixel(1 / scale);
				primaryStage.setViewport(viewport);
				timeAndDateStage.setViewport(viewport);
				onResize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				infoWindow.guiScaleChanged(scale);
				return true;
			}
			case MessageType.GUI_SWITCH_INTERACTION_MODE: {
				GameInteractionMode targetMode = (GameInteractionMode)msg.extraInfo;
				cursorManager.switchToCursor(targetMode.cursorName);
				interactionStateContainer.setInteractionMode(targetMode);
				return true;
			}
			case MessageType.GUI_SWITCH_VIEW_MODE: {
				GameViewMode targetMode = (GameViewMode)msg.extraInfo;
				interactionStateContainer.setGameViewMode(targetMode);
				return true;
			}
			case MessageType.GUI_CANCEL_CURRENT_VIEW: {
				GuiViewName parentViewName = currentView.getParentViewName();
				if (parentViewName != null) {
					switchView(parentViewName);
				} else {
				}
				return true;
			}
			case MessageType.GUI_CANCEL_CURRENT_VIEW_OR_GO_TO_MAIN_MENU: {
				GuiViewName parentViewName = currentView.getParentViewName();
				if (currentViewName.equals(GuiViewName.DEFAULT_MENU)) {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_MENU");
				} else if (parentViewName != null) {
					switchView(parentViewName);
				} else {
					Logger.error("Don't know how to cancel current view from " + currentViewName.name());
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public void update(float deltaTime) {
		timeDateGuiView.update();
		minimapGuiView.update();
		hintGuiView.update();

		primaryStage.act(deltaTime);
		timeAndDateStage.act(deltaTime);

		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > 1f) {
			timeSinceLastUpdate = 0;
			if (currentView != null) {
				currentView.update();
			}
		}
	}

	public void render() {
		primaryStage.draw();
		timeAndDateStage.draw();
		infoWindow.render();
	}

	public void onResize(int width, int height) {
		primaryStage.getViewport().update(width, height, true);
		infoWindow.onResize(width, height);
	}

	public List<InputProcessor> getInputProcessors() {
		return Arrays.asList(primaryStageInputHandler, timeAndDateStageInputHandler);
	}

	private void switchView(GuiViewName viewName) {
		GuiView newView = guiViewRepository.getByName(viewName);
		if (newView == null) {
			Logger.error("No GuiView defined for " + viewName.name());
		} else {
			this.currentViewName = viewName;
			if (currentView != null) {
				currentView.onClose();
			}
			containerTable.clear();
			newView.populate(containerTable);
		}
		currentView = newView;
		if (GlobalSettings.DEV_MODE) {
			debugGuiView.update();
		}
	}

	public void showDialog(GameDialog dialog) {
		dialog.show(primaryStage);
	}

	public void showTooltip(Tooltip tooltip) {
		primaryStage.addActor(tooltip);
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		if (gameContext != null && gameContext.getSettlementState().getGameState().equals(GameState.SELECT_SPAWN_LOCATION)) {
			switchView(GuiViewName.SELECT_STARTING_LOCATION);
		} else {
			switchView(GuiViewName.DEFAULT_MENU);
		}
	}

	@Override
	public void clearContextRelatedState() {

	}
}
