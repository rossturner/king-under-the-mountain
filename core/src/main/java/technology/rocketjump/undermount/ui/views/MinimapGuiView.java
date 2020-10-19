package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.mapping.minimap.MinimapManager;
import technology.rocketjump.undermount.mapping.minimap.MinimapWidget;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nTextButton;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;

@Singleton
public class MinimapGuiView implements GuiView, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final MinimapManager minimapManager;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final MinimapWidget minimapWidget;
	private final Texture minimapSelectionTexture;
	private final I18nTextButton minimapToggleButton;
	private Table table;
	private Boolean minimapDisplayed = true;

	@Inject
	public MinimapGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						  MinimapManager minimapManager, PrimaryCameraWrapper primaryCameraWrapper,
						  I18nWidgetFactory i18nWidgetFactory) {
		this.messageDispatcher = messageDispatcher;
		this.minimapManager = minimapManager;
		this.primaryCameraWrapper = primaryCameraWrapper;

		Skin uiSkin = guiSkinRepository.getDefault();
		table = new Table(uiSkin);

		minimapWidget = new MinimapWidget();

		minimapToggleButton = i18nWidgetFactory.createTextButton("GUI.MINIMAP.LABEL");
		minimapToggleButton.setTouchable(Touchable.enabled);
		minimapToggleButton.addListener(new ClickListener() {
			public void clicked (InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.SHOW_MINIMAP, !minimapDisplayed);
			}
		});

		minimapSelectionTexture = new Texture("assets/ui/minimapSelection.png");
		minimapWidget.setSelectionDrawable(new TextureRegionDrawable(new TextureRegion(minimapSelectionTexture)));
		minimapWidget.setTouchable(Touchable.enabled);

		minimapWidget.addListener(new ClickListener() {
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, new Vector2(
						(x / minimapWidget.getWidth()) * minimapWidget.getMapWidth(),
						(y / minimapWidget.getHeight()) * minimapWidget.getMapHeight()
				));
				return super.touchDown(event, x, y, pointer, button);
			}

			@Override
			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, new Vector2(
						(x / minimapWidget.getWidth()) * minimapWidget.getMapWidth(),
						(y / minimapWidget.getHeight()) * minimapWidget.getMapHeight()
				));
				super.touchDragged(event, x, y, pointer);
			}
		});

		messageDispatcher.addListener(this, MessageType.SHOW_MINIMAP);
	}

	@Override
	public void populate(Table containerTable) {
		resetTable();
		containerTable.add(table).right();
	}

	@Override
	public void update() {
		if (!minimapDisplayed) {
			return;
		}
		if (minimapManager.getCurrentTexture() != null && minimapDisplayed) {
			minimapWidget.setDrawable(new TextureRegionDrawable(new TextureRegion(minimapManager.getCurrentTexture())));
		}
		minimapWidget.setMapSize(minimapManager.getWidth(), minimapManager.getHeight());

		OrthographicCamera camera = primaryCameraWrapper.getCamera();
		minimapWidget.setCameraPosition(camera.position);
		minimapWidget.setViewportSize(camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
	}

	@Override
	public GuiViewName getName() {
		// This is a special case GuiView which lives outside of the normal usage
		return null;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SHOW_MINIMAP: {
				this.minimapDisplayed = (Boolean) msg.extraInfo;
				resetTable();
				update();
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void resetTable() {
		table.clearChildren();
		table.add(minimapToggleButton).right().row();
		if (minimapDisplayed) {
			table.add(minimapWidget).right();
		}
	}

}
