package technology.rocketjump.undermount.rendering.entities;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.undermount.assets.entities.model.AttachmentDescriptor;
import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.assets.entities.model.EntityChildAssetDescriptor;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.AttachedEntity;

import java.util.*;

public class EntityRenderSteps {

	// This TreeMap is used for sorting of entity parts by layer, for drawing in back-to-front order
	private final TreeMap<Integer, EntityPartRenderStep> partsToRender;
	private final HashMap<EntityAssetType, AttachmentDescriptor> attachmentPoints;
	private final HashMap<EntityAssetType, Entity> attachedEntities;

	public EntityRenderSteps clone() {
		return new EntityRenderSteps(
			(TreeMap) this.partsToRender.clone(),
				(HashMap) attachmentPoints.clone(),
				(HashMap) attachedEntities.clone()
		);
	}

	public EntityRenderSteps(TreeMap<Integer, EntityPartRenderStep> partsToRender,
							 HashMap<EntityAssetType, AttachmentDescriptor> attachmentPoints,
							 HashMap<EntityAssetType, Entity> attachedEntities) {
		this.partsToRender = partsToRender;
		this.attachmentPoints = attachmentPoints;
		this.attachedEntities = attachedEntities;
	}

	public EntityRenderSteps() {
		partsToRender = new TreeMap<>();
		attachmentPoints = new HashMap<>();
		attachedEntities = new HashMap<>();
	}

	public void clear() {
		partsToRender.clear();
		attachmentPoints.clear();
		attachedEntities.clear();
	}

	public Collection<EntityPartRenderStep> getRenderSteps() {
		return partsToRender.values();
	}

	public void addPartToRender(int renderLayer, EntityPartRenderStep entityPartRenderStep) {
		partsToRender.put(renderLayer, entityPartRenderStep);
	}

	public void addAttachmentPoints(List<EntityChildAssetDescriptor> attachmentPointList, Vector2 parentPosition) {
		for (EntityChildAssetDescriptor attachmentPoint : attachmentPointList) {
			attachmentPoints.put(attachmentPoint.getType(), new AttachmentDescriptor(attachmentPoint, parentPosition));
		}
	}

	public void addAttachedEntity(AttachedEntity attachedEntity) {
		attachedEntities.put(attachedEntity.holdPosition.getAttachmentType(), attachedEntity.entity);
	}

	public Set<Map.Entry<EntityAssetType, Entity>> getAttachedEntities() {
		return attachedEntities.entrySet();
	}

	public AttachmentDescriptor getAttachmentPoint(EntityAssetType key) {
		return attachmentPoints.get(key);
	}

}
