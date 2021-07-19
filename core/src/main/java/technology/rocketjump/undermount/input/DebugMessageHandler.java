package technology.rocketjump.undermount.input;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.factories.OngoingEffectAttributesFactory;
import technology.rocketjump.undermount.entities.factories.OngoingEffectEntityFactory;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.EntityType;
import technology.rocketjump.undermount.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.TileExploration;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.DebugMessage;
import technology.rocketjump.undermount.rendering.camera.GlobalSettings;

import java.util.ArrayList;

@Singleton
public class DebugMessageHandler implements GameContextAware, Telegraph, Disposable {

	private final MessageDispatcher messageDispatcher;
	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary materialDictionary;

	private final OngoingEffectAttributesFactory ongoingEffectAttributesFactory;
	private final OngoingEffectEntityFactory ongoingEffectEntityFactory;

	private GameContext gameContext;

	@Inject
	public DebugMessageHandler(MessageDispatcher messageDispatcher, ItemTypeDictionary itemTypeDictionary,
							   GameMaterialDictionary materialDictionary, OngoingEffectAttributesFactory ongoingEffectAttributesFactory,
							   OngoingEffectEntityFactory ongoingEffectEntityFactory) {
		this.messageDispatcher = messageDispatcher;
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;
		this.ongoingEffectAttributesFactory = ongoingEffectAttributesFactory;
		this.ongoingEffectEntityFactory = ongoingEffectEntityFactory;

		messageDispatcher.addListener(this, MessageType.DEBUG_MESSAGE);
	}

	@Override
	public void dispose() {
		messageDispatcher.removeListener(this, MessageType.DEBUG_MESSAGE);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.DEBUG_MESSAGE: {
				if (GlobalSettings.DEV_MODE) {
					DebugMessage message = (DebugMessage) msg.extraInfo;
					MapTile tile = gameContext.getAreaMap().getTile(message.getWorldPosition());

					if (tile != null) {

//						messageDispatcher.dispatchMessage(MessageType.YEAR_ELAPSED);

						for (Entity entity : new ArrayList<>(tile.getEntities())) {
							if (entity.getType().equals(EntityType.HUMANOID)) {
//								StatusComponent statusComponent = entity.getOrCreateComponent(StatusComponent.class);
//								statusComponent.apply(new OnFireStatus());
							}
						}

						messageDispatcher.dispatchMessage(MessageType.SPREAD_FIRE_FROM_LOCATION, message.getWorldPosition());

//						ongoingEffectEntityFactory.create(ongoingEffectAttributesFactory.createByTypeName("Fire"),
//							message.getWorldPosition(), gameContext);


//						messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(itemTypeDictionary.getByName("Product-Barrel"), (entity) -> {
//							LiquidContainerComponent liquidContainerComponent = new LiquidContainerComponent();
//							liquidContainerComponent.init(entity, messageDispatcher, gameContext);
//							liquidContainerComponent.setLiquidQuantity(6);
//							liquidContainerComponent.setTargetLiquidMaterial(materialDictionary.getByName("Beer"));
//							entity.addComponent(liquidContainerComponent);
//							entity.getLocationComponent().setWorldPosition(message.getWorldPosition(), false);
//						}));


						if (tile.getExploration().equals(TileExploration.UNEXPLORED)) {
							messageDispatcher.dispatchMessage(MessageType.FLOOD_FILL_EXPLORATION, tile.getTilePosition());
						}

					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
