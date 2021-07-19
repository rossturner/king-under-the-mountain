package technology.rocketjump.undermount.assets.entities.model;

import com.badlogic.gdx.math.Vector2;

import static technology.rocketjump.undermount.assets.entities.model.EntityChildAssetDescriptor.UNSPECIFIED_CHILD_ASSET;
import static technology.rocketjump.undermount.rendering.entities.EntityRenderer.PIXELS_PER_TILE;

public class AttachmentDescriptor {

	private final EntityAssetType attachmentType;
	private final Vector2 offsetPosition;
	private final Integer overrideRenderLayer;

	public static final AttachmentDescriptor UNSPECIFIED_ATTACHMENT = new AttachmentDescriptor(
			UNSPECIFIED_CHILD_ASSET, new Vector2()
	);

	public AttachmentDescriptor(EntityChildAssetDescriptor attachmentDescriptor, Vector2 parentPosition) {
		this.attachmentType = attachmentDescriptor.getType();
		this.overrideRenderLayer = attachmentDescriptor.getOverrideRenderLayer();
		this.offsetPosition = parentPosition.cpy().add(attachmentDescriptor.getOffsetPixels().cpy().scl(1f / PIXELS_PER_TILE));
	}

	public EntityAssetType getAttachmentType() {
		return attachmentType;
	}

	public Vector2 getOffsetPosition() {
		return offsetPosition;
	}

	public Integer getOverrideRenderLayer() {
		return overrideRenderLayer;
	}
}
