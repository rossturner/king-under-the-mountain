package technology.rocketjump.undermount.assets.entities.model;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.undermount.rendering.RenderMode;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SpriteDescriptor {

	private static final float EPSILION = 0.001f;
	private String filename; // Only to be used during data load, not during game loop
	private ColoringLayer coloringLayer = null;
	private boolean isAnimated;
	@JsonIgnore
	private Map<RenderMode, Sprite> renderModeSprites = new EnumMap<>(RenderMode.class);
	@JsonIgnore
	private Map<RenderMode, Array<Sprite>> renderModeAnimatedSprites = new EnumMap<>(RenderMode.class);
	private float scale = 1.0f;
	private StorableVector2 offsetPixels = new StorableVector2();
	private boolean flipX = false;
	private boolean flipY = false;
	// TODO Some property to specify that this completely obscures a lower layer e.g. clothes with no skin showing
	private List<EntityChildAssetDescriptor> childAssets = new LinkedList<>();
	private List<EntityChildAssetDescriptor> attachmentPoints = new LinkedList<>();
	private List<EntityChildAssetDescriptor> parentEntityAssets = new LinkedList<>();

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public ColoringLayer getColoringLayer() {
		return coloringLayer;
	}

	public void setColoringLayer(ColoringLayer coloringLayer) {
		this.coloringLayer = coloringLayer;
	}

	public void setSprite(RenderMode renderMode, Sprite sprite) {
		renderModeSprites.put(renderMode, sprite);
	}

	public Sprite getSprite(RenderMode renderMode) {
		return renderModeSprites.get(renderMode);
	}

	public void setAnimatedSprites(RenderMode renderMode, Array<Sprite> sprites) {
		renderModeAnimatedSprites.put(renderMode, sprites);
	}

	public Array<Sprite> getAnimatedSprites(RenderMode renderMode) {
		return renderModeAnimatedSprites.get(renderMode);
	}

	public StorableVector2 getOffsetPixels() {
		return offsetPixels;
	}

	public void setOffsetPixels(StorableVector2 offsetPixels) {
		this.offsetPixels = offsetPixels;
	}

	public boolean isFlipX() {
		return flipX;
	}

	public void setFlipX(boolean flipX) {
		this.flipX = flipX;
	}

	public boolean isFlipY() {
		return flipY;
	}

	public void setFlipY(boolean flipY) {
		this.flipY = flipY;
	}

	public void setChildAssets(List<EntityChildAssetDescriptor> childAssets) {
		this.childAssets = childAssets;
	}

	public List<EntityChildAssetDescriptor> getChildAssets() {
		return childAssets;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		if (scale < EPSILION) {
			this.scale = 1.0f;
		} else {
			this.scale = scale;
		}
	}

	public List<EntityChildAssetDescriptor> getAttachmentPoints() {
		return attachmentPoints;
	}

	public void setAttachmentPoints(List<EntityChildAssetDescriptor> attachmentPoints) {
		this.attachmentPoints = attachmentPoints;
	}

	public List<EntityChildAssetDescriptor> getParentEntityAssets() {
		return parentEntityAssets;
	}

	public void setParentEntityAssets(List<EntityChildAssetDescriptor> parentEntityAssets) {
		this.parentEntityAssets = parentEntityAssets;
	}


	public boolean getIsAnimated() {
		return isAnimated;
	}

	public void setIsAnimated(boolean animated) {
		isAnimated = animated;
	}
}
