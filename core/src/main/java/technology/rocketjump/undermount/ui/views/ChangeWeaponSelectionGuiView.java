package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ExampleItemDictionary;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.jobs.ProfessionDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ChangeWeaponSelectionMessage;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.undermount.ui.widgets.ImageButton;
import technology.rocketjump.undermount.ui.widgets.ImageButtonFactory;

import java.util.List;
import java.util.Optional;

/**
 * This class is used to select from the professions a settler can change to.
 * Just allowing all professions for now
 */
@Singleton
public class ChangeWeaponSelectionGuiView implements GuiView {

	private final int ITEMS_PER_ROW = 5;
	private final ProfessionDictionary professionDictionary;
	private final Skin uiSkin;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final ItemTypeDictionary itemTypeDictionary;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final ExampleItemDictionary exampleItemDictionary;
	private final ImageButtonFactory imageButtonFactory;
	private final MessageDispatcher messageDispatcher;
	private final ImageButton unarmedButton;

	private Table viewTable;
	private Table weaponTable;
	private ScrollPane scrollPane;

	private final Label headingLabel;
	private final TextButton backButton;


	@Inject
	public ChangeWeaponSelectionGuiView(ProfessionDictionary professionDictionary, I18nWidgetFactory i18nWidgetFactory,
										GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
										GameInteractionStateContainer gameInteractionStateContainer,
										ItemTypeDictionary itemTypeDictionary, ExampleItemDictionary exampleItemDictionary, ImageButtonFactory imageButtonFactory) {
		this.professionDictionary = professionDictionary;
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;

		this.uiSkin = guiSkinRepository.getDefault();
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.itemTypeDictionary = itemTypeDictionary;
		this.exampleItemDictionary = exampleItemDictionary;
		this.imageButtonFactory = imageButtonFactory;

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");

		headingLabel = i18nWidgetFactory.createLabel("GUI.CHANGE_WEAPON_LABEL");
		viewTable.add(headingLabel).center();
		viewTable.row();

		weaponTable = new Table(uiSkin);

		scrollPane = new ScrollPane(weaponTable, uiSkin);
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
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
			}
		});
		viewTable.add(backButton).pad(10).left();
		this.unarmedButton = imageButtonFactory.getOrCreate("punch").clone();
	}

	@Override
	public void populate(Table containerTable) {
		weaponTable.clear();

		int numAdded = 0;

		List<ItemType> allWeapons = itemTypeDictionary.getAllWeapons();

		for (ItemType weaponItemType : allWeapons) {
			Table innerTable = new Table(uiSkin);

			Entity itemEntity = exampleItemDictionary.getExampleItemEntity(weaponItemType, Optional.empty());
			ImageButton imageButton = imageButtonFactory.getOrCreate(itemEntity);
			imageButton.setAction(() -> {
				messageDispatcher.dispatchMessage(MessageType.CHANGE_WEAPON_SELECTION, new ChangeWeaponSelectionMessage(
						gameInteractionStateContainer.getSelectable().getEntity(), weaponItemType
				));
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
			});
			innerTable.add(imageButton).pad(10).row();
			innerTable.add(i18nWidgetFactory.createLabel(weaponItemType.getI18nKey()));

			weaponTable.add(innerTable);
			numAdded++;

			if (numAdded % ITEMS_PER_ROW == 0) {
				weaponTable.row();
			}
		}

		// also add unarmed as final
		Table innerTable = new Table(uiSkin);
		unarmedButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.CHANGE_WEAPON_SELECTION, new ChangeWeaponSelectionMessage(
					gameInteractionStateContainer.getSelectable().getEntity(), null
			));
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
		});
		innerTable.add(unarmedButton).pad(10).row();
		innerTable.add(i18nWidgetFactory.createLabel("WEAPON.UNARMED"));
		weaponTable.add(innerTable);

		containerTable.add(viewTable);
	}

	@Override
	public void update() {

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.CHANGE_WEAPON_SELECTION;
	}

	@Override
	public GuiViewName getParentViewName() {
		// General cancel (right-click) de-selects current entity, so can't go back to entity selected view, just go back to default menu
		return GuiViewName.DEFAULT_MENU;
	}
}
