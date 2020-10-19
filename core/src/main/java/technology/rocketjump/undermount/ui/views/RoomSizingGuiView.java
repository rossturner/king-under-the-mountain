package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.ui.GameInteractionMode;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ButtonStyle;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

@Singleton
public class RoomSizingGuiView implements GuiView {

	private final TextButton backButton;
	private final IconButton addTilesButton;
	private final IconButton removeTilesButton;
	private final IconButton furnitureButton;
	private final Label headingLabel;
	private Table viewTable;

	private Table buttonTable;

	private RoomType currentRoomType;

	@Inject
	public RoomSizingGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							 IconButtonFactory iconButtonFactory, I18nWidgetFactory i18nWidgetFactory) {
		Skin uiSkin = guiSkinRepository.getDefault();

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");
//		viewTable.setDebug(true);


		headingLabel = i18nWidgetFactory.createLabel("GUI.ADD_REMOVE_TILES");
		viewTable.add(headingLabel).center().colspan(2);
		viewTable.row();

		buttonTable = new Table(uiSkin);
//		buttonTable.setWidth(400);
//		buttonTable.setHeight(300);

		addTilesButton = iconButtonFactory.create("GUI.ADD_TILES", "expand", HexColors.get("#36ba3f"), ButtonStyle.DEFAULT);
		addTilesButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_ROOM);
		});
		buttonTable.add(addTilesButton).pad(20);

		removeTilesButton = iconButtonFactory.create("GUI.REMOVE_TILES", "contract", HexColors.get("#d42828"), ButtonStyle.DEFAULT);
		removeTilesButton.setAction(() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.REMOVE_ROOMS));
		buttonTable.add(removeTilesButton).pad(20);

		furnitureButton = iconButtonFactory.create("GUI.BUILD_FURNITURE", "hammer-nails", HexColors.get("#1a7ce1"), ButtonStyle.DEFAULT);
		furnitureButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ROOM_FURNITURE_SELECTION);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
		});
		buttonTable.add(furnitureButton).pad(20);

		viewTable.add(buttonTable).colspan(2);

		viewTable.row();

		backButton = i18nWidgetFactory.createTextButton("GUI.BACK_LABEL");
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
			}
		});
		viewTable.add(backButton).pad(10).left().colspan(2);


	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ROOM_SIZING;
	}

	@Override
	public GuiViewName getParentViewName() {
		if (currentRoomType != null && currentRoomType.getTags().containsKey("STOCKPILE")) {
			return GuiViewName.STOCKPILE_SELECTION;
		} else {
			return GuiViewName.ROOM_SELECTION;
		}
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();
		containerTable.add(viewTable);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

	public void setCurrentRoomType(RoomType currentRoomType) {
		this.currentRoomType = currentRoomType;
		buttonTable.clear();
		buttonTable.add(addTilesButton).pad(20);
		buttonTable.add(removeTilesButton).pad(20);
		if (currentRoomType != null && !currentRoomType.getFurnitureNames().isEmpty()) {
			buttonTable.add(furnitureButton).pad(20);
		}
	}

}
