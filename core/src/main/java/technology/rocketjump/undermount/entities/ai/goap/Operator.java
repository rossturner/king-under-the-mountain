package technology.rocketjump.undermount.entities.ai.goap;

public enum Operator {

	EQUAL_TO((a, b) -> Math.abs(a - b) < 0.0001f),
	GREATER_THAN((a, b) -> a > b),
	LESS_THAN((a, b) -> a < b),
	GREATER_THAN_OR_EQUAL_TO((a, b) -> a >= b),
	LESS_THAN_OR_EQUAL_TO((a, b) -> a <= b);

	private final Operation operation;

	Operator(Operation operation) {
		this.operation = operation;
	}

	public boolean apply(double a, double b) {
		return operation.apply(a, b);
	}

	private interface Operation {
		boolean apply(double a, double b);
	}

}
