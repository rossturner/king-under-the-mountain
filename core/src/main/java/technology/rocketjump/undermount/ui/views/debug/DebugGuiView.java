package technology.rocketjump.undermount.ui.views.debug;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.undermount.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.undermount.entities.components.InventoryComponent;
import technology.rocketjump.undermount.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.undermount.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.undermount.entities.factories.ItemEntityFactory;
import technology.rocketjump.undermount.entities.factories.SettlerFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.DeathReason;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.environment.WeatherManager;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.JobStore;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileExploration;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.materials.model.GameMaterialType;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.CreatureDeathMessage;
import technology.rocketjump.undermount.messaging.types.DebugMessage;
import technology.rocketjump.undermount.messaging.types.PipeConstructionMessage;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;
import technology.rocketjump.undermount.settlement.ImmigrationManager;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.views.GuiView;
import technology.rocketjump.undermount.ui.views.GuiViewName;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.undermount.entities.model.EntityType.*;

/**
 * As this is intended for dev/debug use, it is not translatable or moddable
 */
@Singleton
public class DebugGuiView implements GuiView, GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final Skin uiSkin;
	private final Label titleLabel;
	private final SelectBox<DebugAction> actionSelect;
	private final SelectBox<ItemType> itemTypeSelect;
	private final ItemTypeDictionary itemTypeDictionary;
	private final SelectBox<GameMaterial> materialSelect;
	private final GameMaterialDictionary materialDictionary;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final ItemEntityFactory itemEntityFactory;
	private final SettlerFactory settlerFactory;
	private final WeatherManager weatherManager;
	private final ImmigrationManager immigrationManager;
	private final JobStore jobStore;
	private Table layoutTable;
	private GameContext gameContext;

	private boolean displayed = false;

	private DebugAction currentAction = DebugAction.NONE;
	private ItemType itemTypeToSpawn;
	private GameMaterial selectedMaterial = GameMaterial.NULL_MATERIAL;

	@Inject
	public DebugGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary materialDictionary,
						ItemEntityAttributesFactory itemEntityAttributesFactory, ItemEntityFactory itemEntityFactory,
						SettlerFactory settlerFactory, WeatherManager weatherManager, ImmigrationManager immigrationManager, JobStore jobStore) {
		this.messageDispatcher = messageDispatcher;
		this.uiSkin = guiSkinRepository.getDefault();
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.itemEntityFactory = itemEntityFactory;
		this.settlerFactory = settlerFactory;
		this.weatherManager = weatherManager;
		this.immigrationManager = immigrationManager;
		this.jobStore = jobStore;

		layoutTable = new Table(uiSkin);

		this.titleLabel = new Label("Debug Menu - press middle mouse button to trigger action", uiSkin);

		this.actionSelect =  new SelectBox<>(uiSkin);
		this.actionSelect.setItems(DebugAction.values());
		this.actionSelect.setSelected(currentAction);
		actionSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				currentAction = actionSelect.getSelected();
				resetMaterialSelect();
				update();
			}
		});

		this.itemTypeSelect = new SelectBox<>(uiSkin);
		Array<ItemType> itemTypes = new Array<>();
		for (ItemType itemType : this.itemTypeDictionary.getAll()) {
			itemTypes.add(itemType);
		}
		itemTypeToSpawn = itemTypes.get(0);
		itemTypeSelect.setItems(itemTypes);
		itemTypeSelect.setSelected(itemTypeToSpawn);
		itemTypeSelect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				ItemType selectedType = itemTypeSelect.getSelected();
				itemTypeToSpawn = selectedType;
				resetMaterialSelect();
				update();
			}
		});

		this.materialSelect = new SelectBox<>(uiSkin);
		materialSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				selectedMaterial = materialSelect.getSelected();
				update();
			}
		});

		messageDispatcher.addListener(this, MessageType.TOGGLE_DEBUG_VIEW);
		messageDispatcher.addListener(this, MessageType.DEBUG_MESSAGE);
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.TOGGLE_DEBUG_VIEW: {
				this.displayed = !this.displayed;
				update();
				return true;
			}
			case MessageType.DEBUG_MESSAGE: {
				if (GlobalSettings.DEV_MODE) {
					DebugMessage message = (DebugMessage) msg.extraInfo;
					MapTile tile = gameContext.getAreaMap().getTile(message.getWorldPosition());

					if (tile != null) {
						handleDebugAction(tile, message.getWorldPosition());
					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void handleDebugAction(MapTile tile, Vector2 worldPosition) {
		switch (currentAction) {
			case SPAWN_ITEM: {

				List<Entity> itemsInTile = tile.getEntities().stream().filter(e -> e.getType().equals(ITEM)).collect(Collectors.toList());

				Optional<Entity> existingItemOfType = itemsInTile.stream()
						.filter(e -> e.getType().equals(ITEM) &&
								((ItemEntityAttributes) e.getPhysicalEntityComponent().getAttributes()).getItemType().equals(itemTypeToSpawn))
						.findAny();

				if (existingItemOfType.isPresent()) {
					ItemEntityAttributes existingAttributes = (ItemEntityAttributes) existingItemOfType.get().getPhysicalEntityComponent().getAttributes();
					if (existingAttributes.getQuantity() < existingAttributes.getItemType().getMaxStackSize()) {
						existingAttributes.setQuantity(existingAttributes.getQuantity() + 1);
						messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, existingItemOfType.get());
					}
				} else if (itemsInTile.isEmpty()) {
					List<GameMaterial> materials = new ArrayList<>();
					for (GameMaterialType materialType : itemTypeToSpawn.getMaterialTypes()) {
						if (materialType.equals(selectedMaterial.getMaterialType())) {
							materials.add(selectedMaterial);
						} else {
							List<GameMaterial> materialsForType = materialDictionary.getByType(materialType);
							materials.add(materialsForType.get(gameContext.getRandom().nextInt(materialsForType.size())));
						}
					}

					ItemEntityAttributes attributes = itemEntityAttributesFactory.createItemAttributes(itemTypeToSpawn, 1, materials);
					itemEntityFactory.create(attributes, tile.getTilePosition(), true, gameContext);
				} else {
					Logger.warn("Blocked spawning of item in tile that already contains an item");
				}
				break;
			}
			case SPAWN_SETTLER: {
				settlerFactory.create(worldPosition, DOWN.toVector2(), null, null, gameContext);
				break;
			}
			case KILL_CREATURE: {
				for (Entity entity : tile.getEntities()) {
					if (entity.getType().equals(CREATURE) && !(entity.getBehaviourComponent() instanceof CorpseBehaviour)) {
						messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(entity, DeathReason.UNKNOWN));
						break;
					}
					if (entity.getType().equals(FURNITURE)) {
						InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
						for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
							if (inventoryEntry.entity.getType().equals(CREATURE) && !(inventoryEntry.entity.getBehaviourComponent() instanceof CorpseBehaviour)) {
								messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(inventoryEntry.entity, DeathReason.UNKNOWN));
								break;
							}
						}

					}
				}
				break;
			}
			case DESTROY_ENTITY: {
				tile.getEntities().stream().findFirst().ifPresent(entity -> {
					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, entity);
				});
				break;
			}
			case TRIGGER_FIRE: {
				messageDispatcher.dispatchMessage(MessageType.START_FIRE_IN_TILE, tile);
				break;
			}
			case TRIGGER_LIGHTNING: {
				weatherManager.triggerStrikeAt(tile);
				break;
			}
			case TRIGGER_NEXT_WEATHER: {
				weatherManager.triggerNextWeather();
				break;
			}
			case TRIGGER_IMMIGRATION: {
				gameContext.getSettlementState().setImmigrantsDue(7);
				immigrationManager.triggerImmigration();
				break;
			}
			case TRIGGER_YEAR_END: {
				gameContext.getGameClock().forceYearChange(messageDispatcher);
				break;
			}
			case TOGGLE_CHANNEL: {
				if (!tile.hasWall()) {
					if (tile.hasChannel()) {
						messageDispatcher.dispatchMessage(MessageType.REMOVE_CHANNEL, tile.getTilePosition());
					} else {
						messageDispatcher.dispatchMessage(MessageType.ADD_CHANNEL, tile.getTilePosition());
					}
				}
				break;
			}
			case TRIGGER_BREAKDOWN: {
				Optional<Entity> settler = tile.getEntities().stream().filter(e -> e.getBehaviourComponent() instanceof SettlerBehaviour).findAny();
				if (settler.isPresent()) {
					settler.get().getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.CAUSE_BREAKDOWN);
				}
				break;
			}
			case TOGGLE_PIPE: {
				if (!tile.hasWall()) {
					if (tile.hasPipe()) {
						messageDispatcher.dispatchMessage(MessageType.REMOVE_PIPE, tile.getTilePosition());
					} else {
						messageDispatcher.dispatchMessage(MessageType.ADD_PIPE, new PipeConstructionMessage(
								tile.getTilePosition(), selectedMaterial));
					}
				}
				break;
			}
			case UNCOVER_UNEXPLORED_AREA: {
				if (tile.getExploration().equals(TileExploration.UNEXPLORED)) {
					messageDispatcher.dispatchMessage(MessageType.FLOOD_FILL_EXPLORATION, tile.getTilePosition());
				}
				break;
			}
			case NONE:
			default:
				// Do nothing
		}
	}

	@Override
	public void populate(Table containerTable) {
		update();
		containerTable.add(this.layoutTable);
	}

	@Override
	public void update() {
		if (gameContext != null) {

			layoutTable.clearChildren();
			if (displayed) {
				layoutTable.background("default-rect");
				layoutTable.add(titleLabel).pad(5).row();
				layoutTable.add(actionSelect).pad(5).left().row();

				if (currentAction.equals(DebugAction.SPAWN_ITEM)) {
					layoutTable.add(itemTypeSelect).pad(5).left().row();
					layoutTable.add(materialSelect).pad(5).left().row();
				} else if (currentAction.equals(DebugAction.TOGGLE_PIPE)) {
					layoutTable.add(materialSelect).pad(5).left().row();
				}
			} else {
				layoutTable.setBackground((Drawable) null);
			}
		}
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
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
	}

	private void resetMaterialSelect() {
		if (currentAction.equals(DebugAction.SPAWN_ITEM)) {
			resetMaterialSelectionForType(itemTypeToSpawn.getPrimaryMaterialType());
		} else if (currentAction.equals(DebugAction.TOGGLE_PIPE)) {
			resetMaterialSelectionForType(GameMaterialType.STONE);
		}
	}

	private void resetMaterialSelectionForType(GameMaterialType materialType) {
		List<GameMaterial> eligibleMaterials = materialDictionary.getByType(materialType);

		Array<GameMaterial> selectOptions = new Array<>();
		eligibleMaterials.forEach(m -> selectOptions.add(m));
		selectedMaterial = selectOptions.get(0);
		materialSelect.setSelected(selectOptions.get(0));
		materialSelect.setItems(selectOptions);
	}
}
