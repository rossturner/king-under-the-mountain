package technology.rocketjump.undermount.guice;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import technology.rocketjump.undermount.messaging.ThreadSafeMessageDispatcher;
import technology.rocketjump.undermount.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.undermount.sprites.NormalTerrainSpriteCacheProvider;
import technology.rocketjump.undermount.sprites.TerrainSpriteCache;

public class UndermountGuiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(TerrainSpriteCache.class).annotatedWith(Names.named("diffuse")).toProvider(DiffuseTerrainSpriteCacheProvider.class);
		bind(TerrainSpriteCache.class).annotatedWith(Names.named("normal")).toProvider(NormalTerrainSpriteCacheProvider.class);

		bind(MessageDispatcher.class).to(ThreadSafeMessageDispatcher.class).asEagerSingleton();
	}

}
