package technology.rocketjump.undermount.gamecontext;

/**
 * Used to generify a class which needs to be updated every frame
 */
public interface Updatable extends GameContextAware {

	void update(float deltaTime);

	boolean runWhilePaused();

}
