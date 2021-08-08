package technology.rocketjump.undermount.entities.components;

public interface InfrequentlyUpdatableComponent extends ParentDependentEntityComponent {

	void infrequentUpdate(double elapsedTime);

}
