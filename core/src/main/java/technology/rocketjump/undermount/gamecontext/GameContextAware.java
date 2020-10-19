package technology.rocketjump.undermount.gamecontext;

/**
 * Note that implementations of this interface are instantiated when the app starts (see UndermountApplicationAdapter)
 */
public interface GameContextAware {

	/**
	 * As well as switching to a different GameContext, this method should also clearContextRelatedState down any state in the implementing class
	 */
	void onContextChange(GameContext gameContext);

	/**
	 * This function is called prior to switching game context, so all existing state can be removed first
	 */
	void clearContextRelatedState();

}
