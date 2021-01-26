package technology.rocketjump.undermount.particles.model;

import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.assets.entities.model.StorableVector2;
import technology.rocketjump.undermount.misc.Name;

public class ParticleEffectType {

	@Name
	private String name;
	private String particleFile;
	private float scale = 1f;
	private EntityAssetOrientation usingParentOrientation;
	private boolean isLooping; // if true this effect will loop endlessly until stopped
	private boolean usesTargetMaterialAsTintColor;
	private boolean isAffectedByLighting;
	private float distanceFromParentEntityOrientation; // effect is initialised according to parent entity position and orientation by this amount (distance)
	private StorableVector2 offsetFromParentEntity;
	private boolean renderBehindParent;
	private boolean attachedToParent; // if true, adjusts world position according to parent entity

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParticleFile() {
		return particleFile;
	}

	public void setParticleFile(String particleFile) {
		this.particleFile = particleFile;
	}

	public boolean getIsLooping() {
		return isLooping;
	}

	public void setIsLooping(boolean isLooping) {
		this.isLooping = isLooping;
	}

	public boolean isUsesTargetMaterialAsTintColor() {
		return usesTargetMaterialAsTintColor;
	}

	public void setUsesTargetMaterialAsTintColor(boolean usesTargetMaterialAsTintColor) {
		this.usesTargetMaterialAsTintColor = usesTargetMaterialAsTintColor;
	}

	public boolean getIsAffectedByLighting() {
		return isAffectedByLighting;
	}

	public void setIsAffectedByLighting(boolean affectedByLighting) {
		isAffectedByLighting = affectedByLighting;
	}

	public float getDistanceFromParentEntityOrientation() {
		return distanceFromParentEntityOrientation;
	}

	public void setDistanceFromParentEntityOrientation(float distanceFromParentEntityOrientation) {
		this.distanceFromParentEntityOrientation = distanceFromParentEntityOrientation;
	}

	public boolean isAttachedToParent() {
		return attachedToParent;
	}

	public void setAttachedToParent(boolean attachedToParent) {
		this.attachedToParent = attachedToParent;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public EntityAssetOrientation getUsingParentOrientation() {
		return usingParentOrientation;
	}

	public void setUsingParentOrientation(EntityAssetOrientation usingParentOrientation) {
		this.usingParentOrientation = usingParentOrientation;
	}

	public StorableVector2 getOffsetFromParentEntity() {
		return offsetFromParentEntity;
	}

	public void setOffsetFromParentEntity(StorableVector2 offsetFromParentEntity) {
		this.offsetFromParentEntity = offsetFromParentEntity;
	}

	public boolean isRenderBehindParent() {
		return renderBehindParent;
	}

	public void setRenderBehindParent(boolean renderBehindParent) {
		this.renderBehindParent = renderBehindParent;
	}
}
