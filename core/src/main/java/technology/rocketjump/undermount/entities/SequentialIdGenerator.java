package technology.rocketjump.undermount.entities;


public class SequentialIdGenerator {

	private static final SequentialIdGenerator instance = new SequentialIdGenerator();

    private long lastId = 1;

    public static long nextId() {
        return instance.instanceNextId();
    }

	public static long lastId() {
		return instance.instanceLastId();
	}

	public static void setLastId(long lastId) {
    	instance.setInstanceLastId(lastId);
	}

	public long instanceNextId() {
    	return lastId++;
	}

	public long instanceLastId() {
		return lastId;
	}

	public void setInstanceLastId(long lastId) {
		this.lastId = lastId;
	}
}
