package technology.rocketjump.undermount.sprites.model;

import com.badlogic.gdx.graphics.g2d.Sprite;

public class QuadrantSprites {

	private final Sprite a, b, c, d;

	public QuadrantSprites(Sprite a, Sprite b, Sprite c, Sprite d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	public QuadrantSprites(Sprite single) {
		this.a = single;
		this.b = null;
		this.c = null;
		this.d = null;
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

}
