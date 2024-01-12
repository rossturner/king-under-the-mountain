package technology.rocketjump.undermount.mapgen.model.input;

public class ShrubType {

	private final String name;
	private final boolean fruit;

	public ShrubType(String name, boolean fruit) {
		this.name = name;
		this.fruit = fruit;
	}

	public String getName() {
		return name;
	}

	public boolean hasFruit() {
		return fruit;
	}
}
