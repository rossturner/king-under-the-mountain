package technology.rocketjump.undermount.sprites.model;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class QuadrantSprites {

	private final Sprite a, b, c, d;

	private final boolean isSingleSprite;

	public QuadrantSprites(Sprite a, Sprite b, Sprite c, Sprite d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		isSingleSprite = false;
	}

	public QuadrantSprites(Sprite single) {
		this.a = single;
		this.b = null;
		this.c = null;
		this.d = null;
		this.isSingleSprite = true;
	}

	public Sprite getA() {
		return a;
	}

	public Sprite getB() {
		return b;
	}

	public Sprite getC() {
		return c;
	}

	public Sprite getD() {
		return d;
	}

	public boolean isSingleSprite() {
		return isSingleSprite;
	}
}
