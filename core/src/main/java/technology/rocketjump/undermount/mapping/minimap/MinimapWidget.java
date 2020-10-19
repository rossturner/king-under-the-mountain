package technology.rocketjump.undermount.mapping.minimap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

public class MinimapWidget extends Widget {

	private Scaling scaling;
	private int align = Align.center;
	private float imageX, imageY, imageWidth, imageHeight;
	private Drawable drawable;
	private float cameraPositionX;
	private float cameraPositionY;
	private float viewportWidth;
	private float viewportHeight;

	private float mapWidth;
	private float mapHeight;
	private Drawable selectionDrawable;

	public MinimapWidget() {
		this.scaling = Scaling.stretch;
		this.align = Align.center;
		setSize(getPrefWidth(), getPrefHeight());
	}

	public void layout () {
		if (drawable == null) return;

		float regionWidth = drawable.getMinWidth();
		float regionHeight = drawable.getMinHeight();
		float width = getWidth();
		float height = getHeight();

		Vector2 size = scaling.apply(regionWidth, regionHeight, width, height);
		imageWidth = size.x;
		imageHeight = size.y;

		if ((align & Align.left) != 0)
			imageX = 0;
		else if ((align & Align.right) != 0)
			imageX = (int)(width - imageWidth);
		else
			imageX = (int)(width / 2 - imageWidth / 2);

		if ((align & Align.top) != 0)
			imageY = (int)(height - imageHeight);
		else if ((align & Align.bottom) != 0)
			imageY = 0;
		else
			imageY = (int)(height / 2 - imageHeight / 2);
	}

	public void draw (Batch batch, float parentAlpha) {
		validate();

		Color color = getColor();
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

		float scaleX = getScaleX();
		float scaleY = getScaleY();

		float imagePositionX = getX() + imageX;
		float imagePositionY = getY() + imageY;

		if (drawable != null) {
			drawable.draw(batch, imagePositionX, imagePositionY, imageWidth * scaleX, imageHeight * scaleY);
		}

		float imageCameraPositionX = (cameraPositionX / mapWidth) * imageWidth;
		float imageCameraPositionY = (cameraPositionY / mapHeight) * imageHeight;
		float imageViewportWidth = (viewportWidth / mapWidth) * imageWidth;
		float imageViewportHeight = (viewportHeight / mapHeight) * imageHeight;

		float cameraImageMinX = imageCameraPositionX - (imageViewportWidth / 2);
		float cameaImageMinY = imageCameraPositionY - (imageViewportHeight / 2);

		selectionDrawable.draw(batch, imagePositionX + cameraImageMinX, imagePositionY + cameaImageMinY, imageViewportWidth, imageViewportHeight);

	}

	public float getMapWidth() {
		return mapWidth;
	}

	public float getMapHeight() {
		return mapHeight;
	}

	public void setDrawable (Skin skin, String drawableName) {
		setDrawable(skin.getDrawable(drawableName));
	}

	/** @param drawable May be null. */
	public void setDrawable (Drawable drawable) {
		if (this.drawable == drawable) return;
		if (drawable != null) {
			if (getPrefWidth() != drawable.getMinWidth() || getPrefHeight() != drawable.getMinHeight()) invalidateHierarchy();
		} else
			invalidateHierarchy();
		this.drawable = drawable;
	}

	/** @return May be null. */
	public Drawable getDrawable () {
		return drawable;
	}

	public void setScaling (Scaling scaling) {
		if (scaling == null) throw new IllegalArgumentException("scaling cannot be null.");
		this.scaling = scaling;
		invalidate();
	}

	public void setAlign (int align) {
		this.align = align;
		invalidate();
	}

	public float getMinWidth () {
		return 0;
	}

	public float getMinHeight () {
		return 0;
	}

	public float getPrefWidth () {
		if (drawable != null) return drawable.getMinWidth();
		return 0;
	}

	public float getPrefHeight () {
		if (drawable != null) return drawable.getMinHeight();
		return 0;
	}

	public float getImageX () {
		return imageX;
	}

	public float getImageY () {
		return imageY;
	}

	public float getImageWidth () {
		return imageWidth;
	}

	public float getImageHeight () {
		return imageHeight;
	}

	public void setCameraPosition(Vector3 position) {
		this.cameraPositionX = position.x;
		this.cameraPositionY = position.y;
	}

	public void setViewportSize(float viewportWidth, float viewportHeight) {
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
	}

	public void setMapSize(int mapWidth, int mapHeight) {
		this.mapWidth = (float) mapWidth;
		this.mapHeight = (float) mapHeight;
	}

	public void setSelectionDrawable(TextureRegionDrawable drawable) {
		this.selectionDrawable = drawable;
	}
}
