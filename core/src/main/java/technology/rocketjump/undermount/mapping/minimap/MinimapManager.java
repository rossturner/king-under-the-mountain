package technology.rocketjump.undermount.mapping.minimap;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.entities.planning.BackgroundTaskManager;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.messaging.MessageType;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Singleton
public class MinimapManager implements Updatable, Telegraph {

	private static final float TIME_BETWEEN_BACKGROUND_UPDATES = 3.141f;

	private final BackgroundTaskManager backgroundTaskManager;
	private final MessageDispatcher messageDispatcher;

	private GameContext gameContext;
	private float timeSinceLastUpdate;
	private int width;
	private int height;
	private Texture minimapTexture;
	private Future<Pixmap> futurePixmap;
	private Boolean minimapDisplayed = true;

	@Inject
	public MinimapManager(BackgroundTaskManager backgroundTaskManager, MessageDispatcher messageDispatcher) {
		this.backgroundTaskManager = backgroundTaskManager;
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, MessageType.WALL_REMOVED);
		messageDispatcher.addListener(this, MessageType.SHOW_MINIMAP);
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > TIME_BETWEEN_BACKGROUND_UPDATES) {
			doUpdate();
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SHOW_MINIMAP: {
				this.minimapDisplayed = (Boolean) msg.extraInfo;
				doUpdate();
				return true;
			}
			case MessageType.WALL_REMOVED: {
				doUpdate();
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void doUpdate() {
		timeSinceLastUpdate = 0f;
		if (!minimapDisplayed) {
			return;
		}
		if (futurePixmap != null) {
			if (futurePixmap.isDone()) {
				try {
					Pixmap pixmap = futurePixmap.get();
					if (this.minimapTexture != null) {
						this.minimapTexture.dispose();
					}
					this.minimapTexture = new Texture(pixmap);
					pixmap.dispose();
				} catch (InterruptedException | ExecutionException e) {
					Logger.error(e);
				} finally {
					futurePixmap = null;
				}
			}
		}
		if (futurePixmap == null) {
			Callable<Pixmap> callable = () -> MinimapPixmapGenerator.generateFrom(gameContext.getAreaMap());
			futurePixmap = backgroundTaskManager.postUntrackedCallable(callable);
		}
	}

	public Texture getCurrentTexture() {
		return minimapTexture;
	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		if (gameContext.getAreaMap() != null) {
			if (futurePixmap != null) {
				futurePixmap.cancel(true);
				futurePixmap = null;
			}
			if (this.minimapTexture != null) {
				this.minimapTexture.dispose();
			}

			Pixmap pixmap = MinimapPixmapGenerator.generateFrom(gameContext.getAreaMap());
			this.minimapTexture = new Texture(pixmap);
			pixmap.dispose();

			this.width = gameContext.getAreaMap().getWidth();
			this.height = gameContext.getAreaMap().getHeight();
		}
	}

	@Override
	public void clearContextRelatedState() {
		if (minimapTexture != null) {
			minimapTexture.dispose();
			minimapTexture = null;
		}
		timeSinceLastUpdate = 0f;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}
}
