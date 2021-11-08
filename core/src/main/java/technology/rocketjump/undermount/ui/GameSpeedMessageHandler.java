package technology.rocketjump.undermount.ui;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.creature.Consciousness;
import technology.rocketjump.undermount.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.undermount.environment.model.GameSpeed;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.settlement.SettlerTracker;
import technology.rocketjump.undermount.ui.widgets.IconButtonFactory;
import technology.rocketjump.undermount.ui.widgets.IconOnlyButton;

import static technology.rocketjump.undermount.gamecontext.GameState.SELECT_SPAWN_LOCATION;
import static technology.rocketjump.undermount.gamecontext.GameState.STARTING_SPAWN;

@Singleton
public class GameSpeedMessageHandler implements Telegraph, GameContextAware {

	private final IconButtonFactory iconButtonFactory;
	private final SettlerTracker settlerTracker;
	private final MessageDispatcher messageDispatcher;

	private GameContext gameContext;
	private boolean overrideSpeedActive;
	private GameSpeed preOverrideSpeed;

	@Inject
	public GameSpeedMessageHandler(MessageDispatcher messageDispatcher, IconButtonFactory iconButtonFactory, SettlerTracker settlerTracker) {
		this.iconButtonFactory = iconButtonFactory;
		this.settlerTracker = settlerTracker;
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, MessageType.SET_GAME_SPEED);
		messageDispatcher.addListener(this, MessageType.SETTLER_FELL_ASLEEP);
		messageDispatcher.addListener(this, MessageType.SETTLER_WOKE_UP);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SETTLER_FELL_ASLEEP: {
				boolean allAsleep = true;
				for (Entity entity : settlerTracker.getLiving()) {
					CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (Consciousness.AWAKE.equals(attributes.getConsciousness())) {
						allAsleep = false;
						break;
					}
				}

				if (allAsleep) {
					preOverrideSpeed = gameContext.getGameClock().getCurrentGameSpeed();
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED4);
					overrideSpeedActive = true;
				}
				return true;
			}
			case MessageType.SETTLER_WOKE_UP: {
				if (overrideSpeedActive) {
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, preOverrideSpeed);
				}
				overrideSpeedActive = false;
				return true;
			}
			case MessageType.SET_GAME_SPEED: {
				if (gameContext == null || gameContext.getSettlementState().getGameState().equals(SELECT_SPAWN_LOCATION) ||
						gameContext.getSettlementState().getGameState().equals(STARTING_SPAWN)) {
					return true;
				}
				GameSpeed selectedSpeed = (GameSpeed) msg.extraInfo;
				if (selectedSpeed.equals(GameSpeed.PAUSED)) {
					if (gameContext.getGameClock().isPaused()) {
						gameContext.getGameClock().setPaused(false);
						messageDispatcher.dispatchMessage(MessageType.GAME_PAUSED, gameContext.getGameClock().isPaused());
						selectedSpeed = gameContext.getGameClock().getCurrentGameSpeed(); // To re-highlight the previously picked speed
					} else {
						gameContext.getGameClock().setPaused(true);
						messageDispatcher.dispatchMessage(MessageType.GAME_PAUSED, gameContext.getGameClock().isPaused());
					}
				} else {
					overrideSpeedActive = false;
					gameContext.getGameClock().setPaused(false);
					messageDispatcher.dispatchMessage(MessageType.GAME_PAUSED, gameContext.getGameClock().isPaused());
					gameContext.getGameClock().setCurrentGameSpeed(selectedSpeed);
				}

				for (IconOnlyButton gameSpeedButton : iconButtonFactory.getIconOnlyButtons()) {
					boolean highlight = gameSpeedButton.gameSpeed.equals(selectedSpeed);
					gameSpeedButton.setHighlighted(highlight);
				}

				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void clearContextRelatedState() {
		preOverrideSpeed = null;
		overrideSpeedActive = false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		messageDispatcher.dispatchMessage(MessageType.GAME_PAUSED, gameContext.getGameClock().isPaused());
	}

}
