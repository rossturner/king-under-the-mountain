package technology.rocketjump.undermount.environment;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.jobs.model.JobTarget;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.roof.TileRoofState;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ParticleRequestMessage;
import technology.rocketjump.undermount.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.undermount.particles.model.ParticleEffectInstance;
import technology.rocketjump.undermount.particles.model.ParticleEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class WeatherManager implements GameContextAware {


	private final ParticleEffectType rainType;
	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;
	private final Map<GridPoint2, ParticleEffectInstance> instancesByTileLocation = new HashMap<>();

	@Inject
	public WeatherManager(ParticleEffectTypeDictionary particleEffectTypeDictionary, MessageDispatcher messageDispatcher) {
		this.rainType = particleEffectTypeDictionary.getByName("Rain");
		this.messageDispatcher = messageDispatcher;
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

		if (particleEffectInstance == null) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
					rainType, Optional.empty(), Optional.of(new JobTarget(mapTile)), (p) -> instancesByTileLocation.put(mapTile.getTilePosition(), p)
			));
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		instancesByTileLocation.clear();
	}
}
