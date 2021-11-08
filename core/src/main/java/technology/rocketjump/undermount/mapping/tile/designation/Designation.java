package technology.rocketjump.undermount.mapping.tile.designation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.jobs.model.JobType;
import technology.rocketjump.undermount.misc.Name;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Designation {

	@Name
	private String designationName;
	private String iconName;
	private String createsJobTypeName;
	private Color selectionColor;
	private Color designationColor;

	@JsonIgnore
	private JobType createsJobType;
	@JsonIgnore
	private Sprite iconSprite;

	private JobPriority defaultJobPriority;
	private boolean appliesToEntities;

	@JsonCreator
	public Designation(@JsonProperty("designationName") String designationName,
					   @JsonProperty("iconName") String iconName,
					   @JsonProperty("createsJobType") String createsJobTypeName,
					   @JsonProperty("selectionColor") Color selectionColor,
					   @JsonProperty("designationColor") Color designationColor,
					   @JsonProperty("defaultJobPriority") JobPriority defaultJobPriority,
					   @JsonProperty("appliesToEntities") Boolean appliesToEntities) {
		this.designationName = designationName;
		this.iconName = iconName;
		this.createsJobTypeName = createsJobTypeName;
		this.selectionColor = selectionColor;
		this.designationColor = designationColor;
		this.defaultJobPriority = defaultJobPriority == null ? JobPriority.NORMAL : defaultJobPriority;
		this.appliesToEntities = appliesToEntities != null && appliesToEntities;
	}

	public String getDesignationName() {
		return designationName;
	}

	public String getIconName() {
		return iconName;
	}

	public Color getSelectionColor() {
		return selectionColor;
	}

	public Color getDesignationColor() {
		return designationColor;
	}

	public Sprite getIconSprite() {
		return iconSprite;
	}

	public void setIconSprite(Sprite iconSprite) {
		this.iconSprite = iconSprite;
	}

	public JobType getCreatesJobType() {
		return createsJobType;
	}

	public String getCreatesJobTypeName() {
		return createsJobTypeName;
	}

	public void setCreatesJobType(JobType createsJobType) {
		this.createsJobType = createsJobType;
	}

	public JobPriority getDefaultJobPriority() {
		return defaultJobPriority;
	}

	public boolean isAppliesToEntities() {
		return appliesToEntities;
	}

	public void setDefaultJobPriority(JobPriority defaultJobPriority) {
		this.defaultJobPriority = defaultJobPriority;
	}
}
