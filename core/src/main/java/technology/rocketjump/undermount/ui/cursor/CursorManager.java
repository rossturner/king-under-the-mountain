package technology.rocketjump.undermount.ui.cursor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.assets.TextureAtlasRepository;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static technology.rocketjump.undermount.assets.TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS;

@Singleton
public class CursorManager {

	private static final int BUFFER_WIDTH_HEIGHT = 64;

	private final TextureAtlas textureAtlas;

	private final Map<String, Cursor> cursorsByName = new HashMap<>();

	@Inject
	public CursorManager(TextureAtlasRepository textureAtlasRepository) {
		this.textureAtlas = textureAtlasRepository.get(GUI_TEXTURE_ATLAS);
		createCursors();
	}

	public void switchToCursor(String cursorName) {
		if (cursorName == null || !cursorsByName.containsKey(cursorName)) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
		} else {
			Gdx.graphics.setCursor(cursorsByName.get(cursorName));
		}
	}

	private void createCursors() {
		FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, BUFFER_WIDTH_HEIGHT, BUFFER_WIDTH_HEIGHT, /* hasDepth */ false, /* hasStencil */ false);
		frameBuffer.begin();

		Camera camera = new OrthographicCamera(64, 64);
		SpriteBatch batch = new SpriteBatch();
		batch.setProjectionMatrix(camera.combined);

		FileHandle cursorsDir = Gdx.files.internal("assets/ui/cursors");
		for (FileHandle cursorFile : cursorsDir.list()) {
			if (cursorFile.name().endsWith(".png")) {
				createCursor(cursorFile.nameWithoutExtension(), cursorFile.path(), batch);
			}
		}


		batch.dispose();
		frameBuffer.end();
		frameBuffer.dispose();
	}

	private void createCursor(String cursorName, String texturePath, SpriteBatch batch) {
		Gdx.gl20.glClearColor(0f, 0f, 0f, 0f); //transparent black
		Gdx.gl20.glClear(GL_COLOR_BUFFER_BIT); //clearContextRelatedState the color buffer

		Texture cursorImg = new Texture(texturePath);

		batch.begin();
		batch.draw(cursorImg, -(BUFFER_WIDTH_HEIGHT/2), -(BUFFER_WIDTH_HEIGHT/2));
		batch.end();

		Pixmap frameBufferPixmap = getFrameBufferPixmap();

		Cursor newCursor = Gdx.graphics.newCursor(frameBufferPixmap, 0, BUFFER_WIDTH_HEIGHT - cursorImg.getHeight());
		cursorsByName.put(cursorName, newCursor);

		frameBufferPixmap.dispose();
		cursorImg.dispose();
	}

	private Pixmap getFrameBufferPixmap() {
		Pixmap frameBufferPixmap = ScreenUtils.getFrameBufferPixmap(0, 0, BUFFER_WIDTH_HEIGHT, BUFFER_WIDTH_HEIGHT);
		// Flip the pixmap upside down
		ByteBuffer pixels = frameBufferPixmap.getPixels();
		int numBytes = BUFFER_WIDTH_HEIGHT * BUFFER_WIDTH_HEIGHT * 4;
		byte[] lines = new byte[numBytes];
		int numBytesPerLine = BUFFER_WIDTH_HEIGHT * 4;
		for (int i = 0; i < BUFFER_WIDTH_HEIGHT; i++) {
			pixels.position((BUFFER_WIDTH_HEIGHT - i - 1) * numBytesPerLine);
			pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
		}
		pixels.clear();
		pixels.put(lines);
		pixels.clear();
		return frameBufferPixmap;
	}


}
