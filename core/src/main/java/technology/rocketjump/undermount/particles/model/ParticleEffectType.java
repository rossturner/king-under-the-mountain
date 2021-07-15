package technology.rocketjump.undermount.particles.model;

import technology.rocketjump.undermount.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.undermount.assets.entities.model.StorableVector2;
import technology.rocketjump.undermount.misc.Name;

public class ParticleEffectType {

	@Name
	private String name;

	// One of the following (particleFile, customImplementation, customShader) must be set
	private String particleFile;
	private String customImplementation;
	private String vertexShaderFile;
	private String fragmentShaderFile;

	private float shaderEffectWidth;
	private float shaderEffectHeight;

	private float scale = 1f;
	private EntityAssetOrientation usingParentOrientation;
	private boolean isLooping; // if true this effect will loop endlessly until stopped
	private boolean usesTargetMaterialAsTintColor;
	private boolean isAffectedByLighting;
	private float distanceFromParentEntityOrientation; // effect is initialised according to parent entity position and orientation by this amount (distance)
	private StorableVector2 offsetFromParentEntity;
	private boolean renderBehindParent;
	private boolean overrideYDepth;
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

	public boolean isOverrideYDepth() {
		return overrideYDepth;
	}

	public void setOverrideYDepth(boolean overrideYDepth) {
		this.overrideYDepth = overrideYDepth;
	}

	public String getCustomImplementation() {
		return customImplementation;
	}

	public void setCustomImplementation(String customImplementation) {
		this.customImplementation = customImplementation;
	}

	public String getVertexShaderFile() {
		return vertexShaderFile;
	}

	public void setVertexShaderFile(String vertexShaderFile) {
		this.vertexShaderFile = vertexShaderFile;
	}

	public String getFragmentShaderFile() {
		return fragmentShaderFile;
	}

	public void setFragmentShaderFile(String fragmentShaderFile) {
		this.fragmentShaderFile = fragmentShaderFile;
	}

	public float getShaderEffectWidth() {
		return shaderEffectWidth;
	}

	public void setShaderEffectWidth(float shaderEffectWidth) {
		this.shaderEffectWidth = shaderEffectWidth;
	}

	public float getShaderEffectHeight() {
		return shaderEffectHeight;
	}

	public void setShaderEffectHeight(float shaderEffectHeight) {
		this.shaderEffectHeight = shaderEffectHeight;
	}
}
