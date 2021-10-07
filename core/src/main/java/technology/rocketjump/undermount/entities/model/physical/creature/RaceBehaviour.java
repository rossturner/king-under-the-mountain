package technology.rocketjump.undermount.entities.model.physical.creature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.undermount.entities.components.BehaviourComponent;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RaceBehaviour {

	private String behaviourName;
	@JsonIgnore
	private Class<? extends BehaviourComponent> behaviourClass;

	private RaceBehaviourGroup group;

	public String getBehaviourName() {
		return behaviourName;
	}

	public void setBehaviourName(String behaviourName) {
		this.behaviourName = behaviourName;
	}

	public Class<? extends BehaviourComponent> getBehaviourClass() {
		return behaviourClass;
	}

	public void setBehaviourClass(Class<? extends BehaviourComponent> behaviourClass) {
		this.behaviourClass = behaviourClass;
	}

	public RaceBehaviourGroup getGroup() {
		return group;
	}

	public void setGroup(RaceBehaviourGroup group) {
		this.group = group;
	}
}
