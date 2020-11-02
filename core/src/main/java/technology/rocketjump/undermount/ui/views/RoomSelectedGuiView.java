package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.ErrorType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.rooms.RoomStore;
import technology.rocketjump.undermount.rooms.components.FarmPlotComponent;
import technology.rocketjump.undermount.rooms.components.RoomComponent;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;
import technology.rocketjump.undermount.ui.Selectable;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ImageButton;
import technology.rocketjump.undermount.ui.widgets.*;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType.CROP;
import static technology.rocketjump.undermount.ui.Selectable.SelectableType.ROOM;
import static technology.rocketjump.undermount.ui.views.GuiViewName.ROOM_FURNITURE_SELECTION;

@Singleton
public class RoomSelectedGuiView implements GuiView, GameContextAware {

	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final IconButton removeButton;
	private final IconButton furnitureButton;
	private final RoomStore roomStore;
	private final GameDialogDictionary gameDialogDictionary;
	private final MessageDispatcher messageDispatcher;
	private Table outerTable;
	private Table descriptionTable;

	// For crops
	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private final SelectBox<String> cropSelect;
	private Map<String, PlantSpecies> cropMapping = new HashMap<>();
	private GameContext gameContext;
	private ImageButton changeRoomNameButton;

	@Inject
	public RoomSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
							   GameInteractionStateContainer gameInteractionStateContainer, GameDialogDictionary gameDialogDictionary, ImageButtonFactory imageButtonFactory, IconButtonFactory iconButtonFactory,
							   RoomStore roomStore, PlantSpeciesDictionary plantSpeciesDictionary) {
		uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.gameDialogDictionary = gameDialogDictionary;
		this.roomStore = roomStore;
		this.plantSpeciesDictionary = plantSpeciesDictionary;

		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);


		cropSelect = new SelectBox<>(uiSkin);
		cropSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				PlantSpecies selectedCrop = cropMapping.get(cropSelect.getSelected());
				Selectable selectable = gameInteractionStateContainer.getSelectable();
				if (selectable != null && selectable.type.equals(ROOM)) {
					Room room = selectable.getRoom();
					FarmPlotComponent farmPlotComponent = room.getComponent(FarmPlotComponent.class);
					if (farmPlotComponent != null) {
						farmPlotComponent.setSelectedCrop(selectedCrop);
					}
				}
			}
		});
		resetCropSelect();

		descriptionTable = new Table(uiSkin);
		descriptionTable.pad(10);

		removeButton = iconButtonFactory.create("GUI.REMOVE_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.SMALL);
		removeButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(ROOM)) {
				messageDispatcher.dispatchMessage(MessageType.REMOVE_ROOM, selectable.getRoom());
			}
		});

		furnitureButton = iconButtonFactory.create("GUI.BUILD_FURNITURE", "hammer-nails", HexColors.get("#1a7ce1"), ButtonStyle.SMALL);
		furnitureButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(ROOM)) {
				messageDispatcher.dispatchMessage(MessageType.GUI_ROOM_TYPE_SELECTED, selectable.getRoom().getRoomType());
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, ROOM_FURNITURE_SELECTION);
			}
		});


		changeRoomNameButton = imageButtonFactory.create("fountain-pen", true);
		changeRoomNameButton.setAction(() -> {
			if (currentSelectable.getRoom() != null) {
				// Grabbing translations here so they're always for the correct language
				I18nText renameRoomDialogTitle = i18nTranslator.getTranslatedString("GUI.DIALOG.RENAME_ROOM_TITLE");
				I18nText descriptionText = i18nTranslator.getTranslatedString("RENAME_DESC");
				I18nText buttonText = i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

//				final GameSpeed currentSpeed = gameContext.getGameClock().getCurrentGameSpeed();
				final boolean performPause = !gameContext.getGameClock().isPaused();
				if (performPause) {
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
				}

				String originalRoomName = currentSelectable.getRoom().getRoomName();

				TextInputDialog textInputDialog = new TextInputDialog(renameRoomDialogTitle, descriptionText, originalRoomName, buttonText, uiSkin, (newRoomName) -> {
					if (performPause) {
						// unpause from forced pause
						messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
					}
					if (!originalRoomName.equals(newRoomName)) {
						try {
							roomStore.rename(currentSelectable.getRoom(), newRoomName);
						} catch (RoomStore.RoomNameCollisionException e) {
							ModalDialog errorDialog = gameDialogDictionary.getErrorDialog(ErrorType.ROOM_NAME_ALREADY_EXISTS);
							messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, errorDialog);
						}
					}
				}, messageDispatcher);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, textInputDialog);
			}
		});
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();
		containerTable.add(outerTable);
	}

	private Selectable currentSelectable;

	@Override
	public void update() {
		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(ROOM)) {
			if (currentSelectable == null || !selectable.equals(currentSelectable)) {
				currentSelectable = selectable;
				doUpdate();
			} else {
				// Still update if it's not a farm plot (due to SelectBox reset on farm plot)
				// FIXME Better to use a dialog for making changes, also gives more space to show info
				FarmPlotComponent farmPlotComponent = currentSelectable.getRoom().getComponent(FarmPlotComponent.class);
				if (farmPlotComponent == null) {
					doUpdate();
				}
			}
		}
	}

	private void doUpdate() {
		outerTable.clear();

		descriptionTable.clear();
		if (currentSelectable != null && currentSelectable.type.equals(ROOM)) {
			Room room = currentSelectable.getRoom();

			boolean requiresFurnitureButton = !room.getRoomType().getFurnitureNames().isEmpty();

			outerTable.add(descriptionTable).left().colspan(requiresFurnitureButton ? 2 : 1).row();
			if (requiresFurnitureButton) {
				outerTable.add(furnitureButton).left().pad(4);
			}
			outerTable.add(removeButton).left().pad(4);


			descriptionTable.add(new Label(room.getRoomName(), uiSkin)).left();
			descriptionTable.add(changeRoomNameButton).left().padLeft(6);
			descriptionTable.add(new Container<>()).expandX().row();

			for (RoomComponent roomComponent : room.getAllComponents()) {
				if (roomComponent instanceof SelectableDescription) {
					for (I18nText description : ((SelectableDescription) roomComponent).getDescription(i18nTranslator, gameContext)) {
						descriptionTable.add(new I18nTextWidget(description, uiSkin, messageDispatcher)).colspan(3).left().row();
					}
				}
			}


			FarmPlotComponent farmPlotComponent = room.getComponent(FarmPlotComponent.class);
			if (farmPlotComponent != null) {
				// Add planting season and crop controls
				cropSelect.setSelected(getTranslatedCrop(farmPlotComponent.getSelectedCrop()));

				descriptionTable.add(cropSelect).colspan(3).left().row();
			}

//			FarmPlotBehaviour farmPlotBehaviour = room.getComponent(FarmPlotBehaviour.class);
//			if (GlobalSettings.DEV_MODE && farmPlotBehaviour != null) {
//				String farmDebug = "Outstanding jobs: " + farmPlotBehaviour.numOutstandingJobs();
//				descriptionTable.add(new Label(farmDebug, uiSkin)).colspan(2).left().row();
//			}
		}
	}

	private String getTranslatedCrop(PlantSpecies selectedCrop) {
		for (Map.Entry<String, PlantSpecies> entry : cropMapping.entrySet()) {
			if (selectedCrop == null && entry.getValue() == null) {
				return entry.getKey();
			} else if (selectedCrop != null && selectedCrop.equals(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ROOM_SELECTED;
	}


	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	private void resetCropSelect() {
		cropMapping.clear();
		Array<String> cropOptions = new Array<>();
		String i18nFallow = i18nTranslator.getTranslatedString("GUI.LEAVE_CROP_FALLOW").toString();
		cropMapping.put(i18nFallow, null);
		cropOptions.add(i18nFallow);
		for (PlantSpecies plantSpecies : plantSpeciesDictionary.getAll()) {
			if (plantSpecies.getPlantType().equals(CROP) && plantSpecies.getSeed() != null) {
				String i18nSeedMaterial = i18nTranslator.getTranslatedString(plantSpecies.getSeed().getSeedMaterial().getI18nKey()).toString();
				cropMapping.put(i18nSeedMaterial, plantSpecies);
				cropOptions.add(i18nSeedMaterial);
			}
		}

		cropSelect.setItems(cropOptions);
		cropSelect.setSelected(i18nFallow);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
