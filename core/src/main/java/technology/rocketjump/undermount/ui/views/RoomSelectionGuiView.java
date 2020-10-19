package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.rooms.RoomTypeDictionary;
import technology.rocketjump.undermount.ui.actions.RoomSelectedAction;
import technology.rocketjump.undermount.ui.actions.SwitchGuiViewAction;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import static technology.rocketjump.undermount.ui.widgets.ButtonStyle.LARGE;

@Singleton
public class RoomSelectionGuiView implements GuiView {

	private final int ROOMS_PER_ROW = 5;
	private Table viewTable;
	private Table roomTable;
	private ScrollPane scrollPane;

	private final Label headingLabel;
	private final TextButton backButton;

	@Inject
	public RoomSelectionGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
								IconButtonFactory iconButtonFactory, RoomTypeDictionary roomTypeDictionary,
								I18nWidgetFactory i18NWidgetFactory) {

		Skin uiSkin = guiSkinRepository.getDefault();

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");

		headingLabel = i18NWidgetFactory.createLabel("GUI.ZONES_LABEL");
		viewTable.add(headingLabel).center();
		viewTable.row();

		roomTable = new Table(uiSkin);
		roomTable.setWidth(400);
		roomTable.setHeight(300);
//		roomTable.setDebug(true);


		IconButton stockpilesButton = iconButtonFactory.create("ROOMS.STOCKPILES", "wooden-crate", HexColors.get("#F2ED5A"), LARGE);
		stockpilesButton.setAction(new SwitchGuiViewAction(GuiViewName.STOCKPILE_SELECTION, messageDispatcher));
		roomTable.add(stockpilesButton).pad(10);
		int numRoomsAdded = 1;

		for (RoomType roomType : roomTypeDictionary.getAll()) {
			if (roomType.getRoomName().equals(RoomTypeDictionary.VIRTUAL_PLACING_ROOM.getRoomName())) {
				continue;
			}
			if (roomType.getTags().containsKey("STOCKPILE")) {
				continue;
			}

			IconButton iconButton = iconButtonFactory.create(roomType.getI18nKey(), roomType.getIconName(), roomType.getColor(), LARGE);
			iconButton.setAction(new RoomSelectedAction(roomType, messageDispatcher));
			roomTable.add(iconButton).pad(10);
			numRoomsAdded++;

			if (numRoomsAdded % ROOMS_PER_ROW == 0) {
				roomTable.row();
			}
		}

		scrollPane = new ScrollPane(roomTable, uiSkin);
		scrollPane.setScrollingDisabled(true, false);
		scrollPane.setForceScroll(false, true);
		ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle(uiSkin.get(ScrollPane.ScrollPaneStyle.class));
		scrollPaneStyle.background = null;
		scrollPane.setStyle(scrollPaneStyle);
		scrollPane.setFadeScrollBars(false);

		viewTable.add(scrollPane);//.height(400);
		viewTable.row();

		backButton = i18NWidgetFactory.createTextButton("GUI.BACK_LABEL");
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.DEFAULT_MENU);
			}
		});
		viewTable.add(backButton).pad(10).left();

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ROOM_SELECTION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
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

}
