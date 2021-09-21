package technology.rocketjump.undermount.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisProgressBar;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.undermount.entities.EntityStore;
import technology.rocketjump.undermount.entities.ai.goap.EntityNeed;
import technology.rocketjump.undermount.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.undermount.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.undermount.entities.behaviour.humanoids.CorpseBehaviour;
import technology.rocketjump.undermount.entities.behaviour.humanoids.SettlerBehaviour;
import technology.rocketjump.undermount.entities.components.*;
import technology.rocketjump.undermount.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.undermount.entities.components.humanoid.*;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.humanoid.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.humanoid.DeathReason;
import technology.rocketjump.undermount.entities.model.physical.humanoid.HumanoidEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.jobs.JobTypeDictionary;
import technology.rocketjump.undermount.jobs.model.Job;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.rendering.utils.ColorMixer;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.rooms.Room;
import technology.rocketjump.undermount.screens.CraftingManagementScreen;
import technology.rocketjump.undermount.ui.GameInteractionStateContainer;
import technology.rocketjump.undermount.ui.Selectable;
import technology.rocketjump.undermount.ui.i18n.I18nString;
import technology.rocketjump.undermount.ui.i18n.I18nText;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.i18n.I18nWord;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.ImageButton;
import technology.rocketjump.undermount.ui.widgets.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.entities.components.ItemAllocation.Purpose.CONTENTS_TO_BE_DUMPED;
import static technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent.MAX_HAPPINESS_VALUE;
import static technology.rocketjump.undermount.entities.model.EntityType.ITEM;
import static technology.rocketjump.undermount.jobs.ProfessionDictionary.NULL_PROFESSION;
import static technology.rocketjump.undermount.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.undermount.ui.Selectable.SelectableType.ENTITY;

@Singleton
public class EntitySelectedGuiView implements GuiView, GameContextAware {

	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final IconButton viewCraftingButton;
	private final IconButton deconstructButton;
	private final IconButton emptyLiquidContainerButton;
	private final EntityStore entityStore;
	private final JobStore jobStore;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final MessageDispatcher messageDispatcher;
	private final JobType haulingJobType;

	private Table outerTable;
	private Table entityDescriptionTable;
	private GameContext gameContext;
	private Label beingDeconstructedLabel;

	private final Table nameTable;
	private final Table professionsTable;
	private final Table needsTable;
	private final Table happinessTable;
	private final Table inventoryTable;

	private final Table upperRow;
	private final Table lowerRow;

	private final I18nLabel inventoryLabel;

	private final Map<EntityNeed, I18nLabel> needLabels;
	private final ImageButtonFactory imageButtonFactory;

	@Inject
	public EntitySelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
								 GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory,
								 EntityStore entityStore, JobStore jobStore, I18nWidgetFactory i18nWidgetFactory, JobTypeDictionary jobTypeDictionary, ImageButtonFactory imageButtonFactory) {
		uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.entityStore = entityStore;
		this.jobStore = jobStore;
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;
		this.imageButtonFactory = imageButtonFactory;

		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);

		entityDescriptionTable = new Table(uiSkin);
		entityDescriptionTable.pad(10);

		haulingJobType = jobTypeDictionary.getByName("HAULING");


		viewCraftingButton = iconButtonFactory.create("GUI.CRAFTING_MANAGEMENT.TITLE", "gears", HexColors.get("#6677FF"), ButtonStyle.SMALL);
		viewCraftingButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(ENTITY)) {
				BehaviourComponent behaviourComponent = selectable.getEntity().getBehaviourComponent();
				if (behaviourComponent instanceof CraftingStationBehaviour) {
					messageDispatcher.dispatchMessage(MessageType.SHOW_SPECIFIC_CRAFTING, ((CraftingStationBehaviour)behaviourComponent).getCraftingType());
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, CraftingManagementScreen.NAME);
				}
			}
		});

		deconstructButton = iconButtonFactory.create("GUI.REMOVE_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.SMALL);
		final EntitySelectedGuiView This = this;
		deconstructButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(ENTITY)) {
				Entity entity = selectable.getEntity();
				ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
				if (constructedEntityComponent != null && !constructedEntityComponent.isBeingDeconstructed()) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, selectable.getEntity());
					This.update();
				}
			}
		});

		emptyLiquidContainerButton = iconButtonFactory.create("GUI.EMPTY_CONTAINER_LABEL", "cardboard-box", HexColors.get("#f4ec78"), ButtonStyle.SMALL);
		emptyLiquidContainerButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (isItemContainingLiquidOnGroundAndNoneAllocated(selectable.getEntity())) {
				Entity entity = selectable.getEntity();
				messageDispatcher.dispatchMessage(MessageType.REQUEST_DUMP_LIQUID_CONTENTS, entity);
				This.update();
			}
		});

		beingDeconstructedLabel = i18nWidgetFactory.createLabel("GUI.FURNITURE_BEING_REMOVED");
		inventoryLabel = i18nWidgetFactory.createLabel("INVENTORY.CONTAINS.LABEL");

		nameTable = new Table(uiSkin);
		professionsTable = new Table(uiSkin);
		needsTable = new Table(uiSkin);
		happinessTable = new Table(uiSkin);
		inventoryTable = new Table(uiSkin);

		upperRow = new Table(uiSkin);
		lowerRow = new Table(uiSkin);

		needLabels = i18nWidgetFactory.createNeedsLabels();
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();

		containerTable.add(outerTable);
	}

	@Override
	public void update() {
		outerTable.clear();
		entityDescriptionTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(ENTITY)) {
			Entity entity = selectable.getEntity();
			if (entity.getType().equals(EntityType.HUMANOID)) {
				buildHumanoidSelectedView(entity);
			} else {
				entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(entity), uiSkin, messageDispatcher)).left().row();

				for (EntityComponent component : entity.getAllComponents()) {
					if (component instanceof SelectableDescription) {
						for (I18nText description : ((SelectableDescription) component).getDescription(i18nTranslator, gameContext)) {
							if (!description.isEmpty()) {
								entityDescriptionTable.add(new I18nTextWidget(description, uiSkin, messageDispatcher)).left().row();
							}
						}
					}
				}


				InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
				LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
				if (containsSomething(inventoryComponent, liquidContainerComponent)) {
					entityDescriptionTable.add(inventoryLabel).left().row();
					if (inventoryComponent != null) {
						for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
							entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(inventoryEntry.entity), uiSkin, messageDispatcher)).left().row();
						}
					}
					if (liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0) {
						for (I18nText descriptionString : liquidContainerComponent.i18nDescription(i18nTranslator)) {
							entityDescriptionTable.add(new I18nTextWidget(descriptionString, uiSkin, messageDispatcher)).left().row();
						}
					}
				}

				if (entity.getType().equals(EntityType.FURNITURE) && entity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
					FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (furnitureEntityAttributes.getAssignedToEntityId() != null) {
						Entity assignedToEntity = entityStore.getById(furnitureEntityAttributes.getAssignedToEntityId());
						if (assignedToEntity == null) {
							Logger.error("Could not find furniture's assignedTo entity by ID " + furnitureEntityAttributes.getAssignedToEntityId());
						} else {
							entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getAssignedToLabel(assignedToEntity), uiSkin, messageDispatcher)).left().row();
						}
					}
					if (furnitureEntityAttributes.isDestroyed()) {
						entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString(furnitureEntityAttributes.getDestructionCause().i18nKey),
								uiSkin, messageDispatcher)).left().row();
					}
				}

				if (entity.getType().equals(EntityType.PLANT)) {
					PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.isAfflictedByPests()) {
						entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString("CROP.AFFLICTED_BY_PESTS"), uiSkin, messageDispatcher)).left().row();
					}
					if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.CROP)) {
						float harvestProgress = 100f * attributes.estimatedProgressToHarvesting();
						entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getHarvestProgress(harvestProgress), uiSkin, messageDispatcher)).left().row();
					}
				}

				if (entity.getType().equals(ITEM)) {
					Map<I18nText, Integer> haulingCounts = getHaulingTargetDescriptions(entity);
					for (Map.Entry<I18nText, Integer> targetDescriptionEntry : haulingCounts.entrySet()) {
						Map<String, I18nString> replacements = new HashMap<>();
						replacements.put("targetDescription", targetDescriptionEntry.getKey());
						replacements.put("quantity", new I18nWord(String.valueOf(targetDescriptionEntry.getValue())));
						I18nText allocationDescription = i18nTranslator.getTranslatedWordWithReplacements("HAULING.ASSIGNMENT.DESCRIPTION", replacements);
						entityDescriptionTable.add(new I18nTextWidget(allocationDescription, uiSkin, messageDispatcher)).left().row();
					}

					ItemEntityAttributes itemEntityAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (itemEntityAttributes.isDestroyed()) {
						entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString(itemEntityAttributes.getDestructionCause().i18nKey),
								uiSkin, messageDispatcher)).left().row();
					}

				}

				if (GlobalSettings.DEV_MODE) {
					if (entity.getType().equals(ITEM)) {
						ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
						List<ItemAllocation> itemAllocations = itemAllocationComponent.getAll();
						if (itemAllocations.size() > 0) {
							String allocationsString = StringUtils.join(itemAllocations, ", ");
							entityDescriptionTable.add(new Label("Allocations: " + allocationsString, uiSkin)).left();
						}
					} else if (entity.getType().equals(EntityType.HUMANOID)) {
//					SettlerBehaviour behaviourComponent = (SettlerBehaviour) entity.getBehaviourComponent();
//					if (behaviourComponent.getCurrentGoal() != null) {
//						String goal = "Goal: " + behaviourComponent.getCurrentGoal().goal.name;
//						entityDescriptionTable.add(new Label(goal, uiSkin)).left();
//						entityDescriptionTable.row();
//
//						if (behaviourComponent.getCurrentGoal().getCurrentAction() != null) {
//							String action = "Action: " + behaviourComponent.getCurrentGoal().getCurrentAction().getClass().getSimpleName();
//							entityDescriptionTable.add(new Label(action, uiSkin)).left();
//							entityDescriptionTable.row();
//						}
//
//						GoalQueue goalQueue = behaviourComponent.getGoalQueue();
//						entityDescriptionTable.add(new Label("Queued: " + goalQueue.toString(), uiSkin)).left().row();
//					}
					} else if (entity.getType().equals(EntityType.PLANT)) {
//					PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
//
//					PlantSpeciesGrowthStage currentGrowthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());
//					String growthStageDescription = "GS: " + currentGrowthStage.getName() + " progress: " + df.format(attributes.getGrowthStageProgress());
//					entityDescriptionTable.add(new Label(growthStageDescription, uiSkin)).left().row();
//
//					BehaviourComponent behaviourComponent = entity.getBehaviourComponent();
//					if (behaviourComponent instanceof PlantBehaviour) {
//						entityDescriptionTable.add(new Label("Season: " + ((PlantBehaviour) behaviourComponent).getSeasonPlantThinksItIs() +
//								" tTN: "+df.format(((PlantBehaviour) behaviourComponent).getGameSeasonsToNoticeSeasonChange()) +
//								" progress: " + df.format(attributes.getSeasonProgress()), uiSkin)).left().row();
//					}
					}
				}
			}

			entityDescriptionTable.row();

			outerTable.add(entityDescriptionTable);

			if (entity.getBehaviourComponent() != null && entity.getBehaviourComponent() instanceof CraftingStationBehaviour) {
				outerTable.add(viewCraftingButton);
			}

			ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
			if (constructedEntityComponent != null) {
				if (constructedEntityComponent.isBeingDeconstructed()) {
					outerTable.add(beingDeconstructedLabel);
				} else if (constructedEntityComponent.canBeDeconstructed()) {
					outerTable.add(deconstructButton);
				}
			}

			if (isItemContainingLiquidOnGroundAndNoneAllocated(entity)) {
				outerTable.add(emptyLiquidContainerButton);
			}

			ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent != null && itemAllocationComponent.getAllocationForPurpose(CONTENTS_TO_BE_DUMPED) != null) {
				outerTable.add(i18nWidgetFactory.createLabel("GUI.EMPTY_CONTAINER_LABEL.BEING_ACTIONED"));
			}
		}
	}

	private void buildHumanoidSelectedView(Entity entity) {

		nameTable.clear();
		professionsTable.clear();
		needsTable.clear();
		inventoryTable.clear();
		happinessTable.clear();

		upperRow.clear();
		lowerRow.clear();

		populateSettlerNameTable(entity, nameTable, i18nTranslator, uiSkin, gameContext, messageDispatcher, imageButtonFactory);

		InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
		if (containsSomething(inventoryComponent, null)) {
			inventoryTable.add(inventoryLabel).left().row();

			for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
				inventoryTable.add(new I18nTextWidget(i18nTranslator.getDescription(inventoryEntry.entity), uiSkin, messageDispatcher)).left().row();
			}
		}

		HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		if (attributes.getConsciousness().equals(Consciousness.DEAD)) {
			upperRow.add(nameTable).top().padRight(5);
			lowerRow.add(inventoryTable).top().padRight(5);
		} else {
			populateProfessionTable(entity);
			populateNeedsTable(needsTable, entity, needLabels, uiSkin);
			populateHappinessTable(entity);

			upperRow.add(nameTable).top().padRight(5);
			upperRow.add(professionsTable).padRight(5);

			lowerRow.add(needsTable).top().padRight(5);
			lowerRow.add(inventoryTable).top().padRight(5);
			lowerRow.add(happinessTable).top().padRight(5);
		}

		entityDescriptionTable.add(upperRow).left().row();
		entityDescriptionTable.add(lowerRow).left();
	}

	public static void populateSettlerNameTable(Entity entity, Table nameTable, I18nTranslator i18nTranslator, Skin uiSkin, GameContext gameContext, MessageDispatcher messageDispatcher, ImageButtonFactory imageButtonFactory) {
		Cell<I18nTextWidget> nameCell = nameTable.add(new I18nTextWidget(i18nTranslator.getDescription(entity), uiSkin, messageDispatcher)).left();

		if (imageButtonFactory != null) {
			ImageButton changeSettlerNameButton = imageButtonFactory.create("fountain-pen", true).clone();
			changeSettlerNameButton.setAction(() -> {
				// Grabbing translations here so they're always for the correct language
				I18nText renameDialogTitle = i18nTranslator.getTranslatedString("GUI.DIALOG.RENAME_SETTLER_TITLE");
				I18nText descriptionText = i18nTranslator.getTranslatedString("RENAME_DESC");
				I18nText buttonText = i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

				final GameSpeed currentSpeed = gameContext.getGameClock().getCurrentGameSpeed();
				final boolean performPause = !gameContext.getGameClock().isPaused();
				if (performPause) {
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
				}

				HumanoidEntityAttributes attributes = (HumanoidEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				String originalName = attributes.getName().toString();

				TextInputDialog textInputDialog = new TextInputDialog(renameDialogTitle, descriptionText, originalName, buttonText, uiSkin, (newName) -> {
					if (performPause) {
						// unpause from forced pause
						messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
					}
					if (!originalName.equals(newName) && !newName.isEmpty()) {
						attributes.getName().rename(newName);
					}
				}, messageDispatcher);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, textInputDialog);
			});
			nameTable.add(changeSettlerNameButton).left().padLeft(5).row();
		} else {
			nameCell.row();
		}

		if (entity.getBehaviourComponent() instanceof SettlerBehaviour) {
			I18nText goalDescription = i18nTranslator.getCurrentGoalDescription(entity, gameContext);
			nameTable.add(new I18nTextWidget(goalDescription, uiSkin, messageDispatcher)).left().row();
		} else if (entity.getBehaviourComponent() instanceof CorpseBehaviour) {
			HistoryComponent historyComponent = entity.getComponent(HistoryComponent.class);
			if (historyComponent != null && historyComponent.getDeathReason() != null) {
				DeathReason reason = historyComponent.getDeathReason();

				Map<String, I18nString> replacements = new HashMap<>();
				replacements.put("reason", i18nTranslator.getDictionary().getWord(reason.getI18nKey()));
				I18nText deathDescriptionString = i18nTranslator.getTranslatedWordWithReplacements("NOTIFICATION.DEATH.SHORT_DESCRIPTION", replacements);
				I18nTextWidget label = new I18nTextWidget(deathDescriptionString, uiSkin, messageDispatcher);
				nameTable.add(label).left().row();
			}
		}
	}

	private void populateProfessionTable(Entity entity) {
		ProfessionsComponent professionsComponent = entity.getComponent(ProfessionsComponent.class);
		if (professionsComponent == null) {
			return;
		}
		int numProfessionsDisplayed = 0;
		List<ProfessionsComponent.QuantifiedProfession> activeProfessions = professionsComponent.getActiveProfessions();

		for (ProfessionsComponent.QuantifiedProfession quantifiedProfession : activeProfessions) {
			if (!quantifiedProfession.getProfession().equals(NULL_PROFESSION) || activeProfessions.size() == 1) {
				ImageButton imageButton = quantifiedProfession.getProfession().getImageButton();
				imageButton.setAction(() -> {
					gameInteractionStateContainer.setProfessionToReplace(quantifiedProfession.getProfession());
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.CHANGE_PROFESSION);
				});
				professionsTable.add(imageButton).pad(5);
				numProfessionsDisplayed++;
			}
		}
		if (!professionsComponent.getPrimaryProfession(NULL_PROFESSION).equals(NULL_PROFESSION)) {
			while (numProfessionsDisplayed < ProfessionsComponent.MAX_PROFESSIONS) {
				ImageButton imageButton = NULL_PROFESSION.getImageButton();
				imageButton.setAction(() -> {
					gameInteractionStateContainer.setProfessionToReplace(null);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.CHANGE_PROFESSION);
				});
				professionsTable.add(imageButton).pad(5);
				numProfessionsDisplayed++;
			}
		}

		professionsTable.row();
		numProfessionsDisplayed = 0;

		for (ProfessionsComponent.QuantifiedProfession quantifiedProfession : activeProfessions) {
			if (!quantifiedProfession.getProfession().equals(NULL_PROFESSION) || activeProfessions.size() == 1) {
				professionsTable.add(i18nWidgetFactory.createLabel(quantifiedProfession.getProfession().getI18nKey())).pad(5);
				numProfessionsDisplayed++;
			}
		}
		if (!professionsComponent.getPrimaryProfession(NULL_PROFESSION).equals(NULL_PROFESSION)) {
			while (numProfessionsDisplayed < ProfessionsComponent.MAX_PROFESSIONS) {
				professionsTable.add(new Container<>()).pad(5); // Pad out
				numProfessionsDisplayed++;
			}
		}
	}

	public static void populateNeedsTable(Table needsTable, Entity entity, Map<EntityNeed, I18nLabel> needLabels, Skin uiSkin) {
		NeedsComponent needsComponent = entity.getComponent(NeedsComponent.class);

		for (Map.Entry<EntityNeed, I18nLabel> entry : needLabels.entrySet()) {
			Double needValue = needsComponent.getValue(entry.getKey());
			if (needValue == null) {
				continue;
			}
			needsTable.add(new I18nLabel(entry.getValue())).pad(5);
			ProgressBar progressBar = new VisProgressBar(0, 100, 1, false);
			progressBar.setValue(Math.round(needValue));
			progressBar.setDisabled(true);
			needsTable.add(progressBar).left().padRight(5);
			needsTable.row();
		}

	}

	private void populateHappinessTable(Entity entity) {
		HappinessComponent happinessComponent = entity.getComponent(HappinessComponent.class);

		Label modifierLabel = buildHappinessModifierLabel(happinessComponent, uiSkin);

		Table headingTable = new Table(uiSkin);
		headingTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString("HAPPINESS_MODIFIER.TITLE"), uiSkin, messageDispatcher));
		headingTable.add(modifierLabel);

		happinessTable.add(headingTable).left().row();

		for (HappinessComponent.HappinessModifier modifier : happinessComponent.currentModifiers()) {
			StringBuilder sb = new StringBuilder();
			sb.append(i18nTranslator.getTranslatedString(modifier.getI18nKey()));
			sb.append(" (");
			int modifierAmount = modifier.modifierAmount;
			if (modifierAmount > 0) {
				sb.append("+");
			}
			sb.append(modifierAmount).append(")");

			happinessTable.add(new Label(sb.toString(), uiSkin)).left().row();
		}

		if (GlobalSettings.DEV_MODE) {
			StatusComponent statusComponent = entity.getComponent(StatusComponent.class);
			if (statusComponent != null && statusComponent.count() > 0) {
				String statuses = "Status: " + statusComponent.getAll().stream().map(s -> s.getClass().getSimpleName()).collect(Collectors.joining(", "));
				happinessTable.add(new Label(statuses, uiSkin)).left().row();
			}
		}
	}

	public static Label buildHappinessModifierLabel(HappinessComponent happinessComponent, Skin uiSkin) {
		StringBuilder modifierBuilder = new StringBuilder();
		int netModifier = happinessComponent.getNetModifier();
		modifierBuilder.append(" ");
		if (netModifier >= 0) {
			modifierBuilder.append("+");
		}
		modifierBuilder.append(netModifier);
		Label modifierLabel = new Label(modifierBuilder.toString(), uiSkin);
		Label.LabelStyle modifierStyle = new Label.LabelStyle(modifierLabel.getStyle());
		if (netModifier >= 0) {
			modifierStyle.fontColor = ColorMixer.interpolate(0, MAX_HAPPINESS_VALUE, netModifier, Color.YELLOW, Color.GREEN);
		} else {
			modifierStyle.fontColor = ColorMixer.interpolate(0, MAX_HAPPINESS_VALUE, -netModifier, Color.YELLOW, Color.RED);
		}
		modifierLabel.setStyle(modifierStyle);
		return modifierLabel;
	}

	private boolean containsSomething(InventoryComponent inventoryComponent, LiquidContainerComponent liquidContainerComponent) {
		return (inventoryComponent != null && !inventoryComponent.getInventoryEntries().isEmpty()) ||
				(liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0);
	}

	private Map<I18nText, Integer> getHaulingTargetDescriptions(Entity itemEntity) {
		Map<I18nText, Integer> haulingTargetDescriptions = new LinkedHashMap<>();

		List<Job> jobsAtLocation = jobStore.getJobsAtLocation(toGridPoint(itemEntity.getLocationComponent().getWorldOrParentPosition()));
		for (Job jobAtLocation : jobsAtLocation) {
			if (jobAtLocation.getType().equals(haulingJobType) && jobAtLocation.getHaulingAllocation().getHauledEntityId() == itemEntity.getId()) {
				I18nText targetDescription = null;
				MapTile targetTile = gameContext.getAreaMap().getTile(jobAtLocation.getHaulingAllocation().getTargetPosition());
				if (targetTile == null) {
					Logger.error("Target tile of hauling allocation is null");
					continue;
				}
				switch (jobAtLocation.getHaulingAllocation().getTargetPositionType()) {
					case ROOM: {
						if (targetTile.getRoomTile() != null) {
							Room room = targetTile.getRoomTile().getRoom();
							targetDescription = new I18nText(room.getRoomName());
						}
						break;
					}
					case CONSTRUCTION: {
						if (targetTile.getConstruction() != null) {
							targetDescription = i18nTranslator.getDescription(targetTile.getConstruction());
						}
						break;
					}
					case FURNITURE: {
						Entity targetEntity = entityStore.getById(jobAtLocation.getHaulingAllocation().getTargetId());
						if (targetEntity != null) {
							targetDescription = i18nTranslator.getDescription(targetEntity);
						}
						break;
					}
					case FLOOR: {
						targetDescription = i18nTranslator.getDescription(targetTile);
					}
					case ZONE:
					default: {
						Logger.error("Not yet implemented: getHaulingTargetDescriptions() for hauling target position type " + jobAtLocation.getHaulingAllocation().getTargetPositionType());
					}
				}
				if (targetDescription != null) {
					int quantity = haulingTargetDescriptions.getOrDefault(targetDescription, 0);
					if (jobAtLocation.getHaulingAllocation().getItemAllocation() != null) {
						quantity += jobAtLocation.getHaulingAllocation().getItemAllocation().getAllocationAmount();
					}
					haulingTargetDescriptions.put(targetDescription, quantity);
				} else {
					Logger.error("Target "+jobAtLocation.getHaulingAllocation().getTargetPositionType()+" of hauling allocation is not found");
				}
			}
		}

		return haulingTargetDescriptions;
	}

	private boolean isItemContainingLiquidOnGroundAndNoneAllocated(Entity entity) {
		if (entity == null) {
			return false;
		}
		LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
		ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
		return entity.getType().equals(ITEM) &&
				(itemAllocationComponent == null || itemAllocationComponent.getNumAllocated() == 0) &&
				liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0 && liquidContainerComponent.getNumAllocated() < 0.001f &&
				((ItemEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getItemPlacement().equals(ItemPlacement.ON_GROUND);
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ENTITY_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

}
