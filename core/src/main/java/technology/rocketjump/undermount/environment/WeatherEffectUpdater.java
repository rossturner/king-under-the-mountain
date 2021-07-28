package technology.rocketjump.undermount.environment;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.environment.model.WeatherType;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ParticleRequestMessage;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Singleton
public class WeatherEffectUpdater implements GameContextAware, Telegraph {


	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;
	private final Map<GridPoint2, ParticleEffectInstance> instancesByTileLocation = new HashMap<>();

	@Inject
	public WeatherEffectUpdater(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, MessageType.GAME_PAUSED);
	}

	public void updateVisibleTile(MapTile mapTile) {
		if (!mapTile.getRoof().getState().equals(TileRoofState.OPEN)) {
			return;
		}

		ParticleEffectInstance particleEffectInstance = instancesByTileLocation.get(mapTile.getTilePosition());
		if (particleEffectInstance != null) {
			if (!particleEffectInstance.isActive()) {
				particleEffectInstance = null;
				instancesByTileLocation.remove(mapTile.getTilePosition());
			}
		}


		WeatherType currentWeather = gameContext.getMapEnvironment().getCurrentWeather();
		if (currentWeather.getParticleEffectType() != null) {
			if (particleEffectInstance == null) {
				messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
						currentWeather.getParticleEffectType(), Optional.empty(),
						Optional.of(new JobTarget(mapTile)), (p) -> instancesByTileLocation.put(mapTile.getTilePosition(), p)
				));
			}
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		Iterator<ParticleEffectInstance> iterator = instancesByTileLocation.values().iterator();
		while (iterator.hasNext()) {
			ParticleEffectInstance instance = iterator.next();
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_FORCE_REMOVE, instance);
			iterator.remove();
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GAME_PAUSED: {
				// Clear all on pause so panning camera doesn't look weird
				clearContextRelatedState();
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

}
