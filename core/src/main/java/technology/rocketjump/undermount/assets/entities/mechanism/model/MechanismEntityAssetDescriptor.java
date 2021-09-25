package technology.rocketjump.undermount.assets.entities.mechanism.model;

import technology.rocketjump.undermount.assets.entities.model.EntityAssetType;
import technology.rocketjump.undermount.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.undermount.misc.Name;

import java.util.List;

public class MechanismEntityAssetDescriptor {

	@Name
	private String uniqueName;
	private EntityAssetType type;
	private String mechanismTypeName;
	private List<String> mechanismTypeNames;
	private Integer layoutId;

	public boolean matches(MechanismEntityAttributes entityAttributes) {
		if (mechanismTypeName != null && !mechanismTypeName.equals(entityAttributes.getMechanismType().getName())) {
			return false;
		}
		if (mechanismTypeNames != null && !mechanismTypeNames.contains(entityAttributes.getMechanismType().getName())) {
			return false;
		}
		if (layoutId != null && entityAttributes.getPipeLayout() != null && !layoutId.equals(entityAttributes.getPipeLayout().getId())) {
			return false;
		}
		return true;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}

	public String getMechanismTypeName() {
		return mechanismTypeName;
	}

	public void setMechanismTypeName(String MechanismTypeName) {
		this.mechanismTypeName = MechanismTypeName;
	}

	public List<String> getMechanismTypeNames() {
		return mechanismTypeNames;
	}

	public void setMechanismTypeNames(List<String> MechanismTypeNames) {
		this.mechanismTypeNames = MechanismTypeNames;
	}

	public Integer getLayoutId() {
		return layoutId;
	}

	public void setLayoutId(Integer layoutId) {
		this.layoutId = layoutId;
	}
}
