package technology.rocketjump.undermount.ui.fonts;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Disposable;

/**
 * This is a wrapper around GDX's BitmapFont to provide access to larger or smaller versions of the same font
 */
public class GameFont implements Disposable {

	private final BitmapFont bitmapFont;
	private final float fontPointSize;
	private GameFont bigger;
	private GameFont smaller;
	private boolean disposed;

	public GameFont(BitmapFont bitmapFont, float fontPointSize) {
		this.bitmapFont = bitmapFont;
		this.fontPointSize = fontPointSize;
	}

	public BitmapFont getBitmapFont() {
		return bitmapFont;
	}

	public GameFont getBigger() {
		return bigger;
	}

	public GameFont getSmaller() {
		return smaller;
	}

	public void setBigger(GameFont bigger) {
		this.bigger = bigger;
	}

	public void setSmaller(GameFont smaller) {
		this.smaller = smaller;
	}

	public float getFontPointSize() {
		return fontPointSize;
	}

	@Override
	public void dispose() {
		if (!disposed) {
			disposed = true;
			bitmapFont.dispose();
			if (bigger != null) {
				bigger.dispose();
			}
			if (smaller != null) {
				smaller.dispose();
			}
		}
	}
}
