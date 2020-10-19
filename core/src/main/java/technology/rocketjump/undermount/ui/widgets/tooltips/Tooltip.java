package technology.rocketjump.undermount.ui.widgets.tooltips;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import technology.rocketjump.undermount.ui.widgets.I18nTextWidget;

public class Tooltip extends Container<I18nTextWidget> {

	private final String i18nKey;
	private TooltipState state;
	private float elapsedTime;
	private Actor parent;
	private float alpha;

	public Tooltip(String i18nKey, I18nTextWidget label, Skin skin, Actor parent) {
		this.i18nKey = i18nKey;
		setActor(label);
		setBackground(skin.getDrawable("default-rect"));
		this.state = TooltipState.PENDING;
		this.parent = parent;
		this.pad(6f);
		setWidth(getMinWidth());
		setHeight(getMinHeight());
	}

	@Override
	public void draw (Batch batch, float parentAlpha) {
		// Just overrides parentAlpha
		validate();
		super.draw(batch, this.alpha);
	}

	public TooltipState getState() {
		return state;
	}

	public void setState(TooltipState state) {
		this.state = state;
		this.elapsedTime = 0f;
	}

	public float getElapsedTime() {
		return elapsedTime;
	}

	public String getText() {
		return this.getActor().getI18nText().toString();
	}

	public Actor getParentActor() {
		return parent;
	}

	public void incrementElapsedTime(float elapsed) {
		elapsedTime += elapsed;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setAlpha(float opacity) {
		this.alpha = Math.min(opacity, 1f);
	}

	public float getAlpha() {
		return alpha;
	}

	public enum TooltipState {

		PENDING,
		DISPLAYED,
		DECAYING

	}
}
