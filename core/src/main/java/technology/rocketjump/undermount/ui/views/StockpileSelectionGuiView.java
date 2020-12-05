package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rooms.RoomType;
import technology.rocketjump.undermount.rooms.RoomTypeDictionary;
import technology.rocketjump.undermount.rooms.StockpileGroup;
import technology.rocketjump.undermount.rooms.StockpileGroupDictionary;
import technology.rocketjump.undermount.ui.actions.RoomSelectedAction;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.IconButton;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;

import static technology.rocketjump.undermount.ui.widgets.ButtonStyle.LARGE;

@Singleton
public class StockpileSelectionGuiView implements GuiView {

	private final int ROOMS_PER_ROW = 4;
	private Table viewTable;
	private Table roomTable;
	private ScrollPane scrollPane;

	private final Label headingLabel;
	private final TextButton backButton;

	@Inject
	public StockpileSelectionGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
									 IconButtonFactory iconButtonFactory, RoomTypeDictionary roomTypeDictionary,
									 I18nWidgetFactory i18nWidgetFactory, StockpileGroupDictionary stockpileGroupDictionary) {
		Skin uiSkin = guiSkinRepository.getDefault();

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");

		headingLabel = i18nWidgetFactory.createLabel("ROOMS.STOCKPILES");
		viewTable.add(headingLabel).center();
		viewTable.row();

		roomTable = new Table(uiSkin);
		roomTable.setWidth(400);
		roomTable.setHeight(300);
//		roomTable.setDebug(true);


		int numRoomsAdded = 0;
		RoomType stockpileRoomType = roomTypeDictionary.getByName("STOCKPILE");

		for (StockpileGroup stockpileGroup : stockpileGroupDictionary.getAll()) {
			IconButton iconButton = iconButtonFactory.create(stockpileGroup.getI18nKey(), stockpileGroup.getIconName(), stockpileGroup.getColor(), LARGE);
			RoomSelectedAction roomSelectedAction = new RoomSelectedAction(stockpileRoomType, messageDispatcher);
			roomSelectedAction.setStockpileGroup(stockpileGroup);
			iconButton.setAction(roomSelectedAction);
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

		backButton = i18nWidgetFactory.createTextButton("GUI.BACK_LABEL");
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ROOM_SELECTION);
			}
		});
		viewTable.add(backButton).pad(10).left();

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.STOCKPILE_SELECTION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.ROOM_SELECTION;
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
