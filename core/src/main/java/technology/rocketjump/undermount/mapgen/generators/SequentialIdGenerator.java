package technology.rocketjump.undermount.mapgen.generators;

public class SequentialIdGenerator {

    private static long lastId = 1;

    public static long nextId() {
        return lastId++;
    }

	public static void reset() {
		lastId = 1;
	}
}
