package org.ihtsdo.buildcloud.core.service.build.transform.conditional;

import java.util.Collection;

public class Condition {

	private Check check;
	private final ConditionalTransformation parent;

	public Condition(ConditionalTransformation conditionalTransformation) {
		this.parent = conditionalTransformation;
	}

	public ConditionalTransformation columnValueInCollection(final int columnIndex, final Collection<String> expectedValues) {
		this.check = new Check() {
			@Override
			boolean isTrue(String[] line) {
				return line.length > columnIndex && expectedValues.contains(line[columnIndex]);
			}
		};
		return parent;
	}

	public ConditionalTransformation columnValueEquals(final int columnIndex, final String expectedValue) {
		this.check = new Check() {
			@Override
			boolean isTrue(String[] line) {
				return line.length > columnIndex && expectedValue.equals(line[columnIndex]);
			}
		};
		return parent;
	}

	public boolean test(String[] columnValues) {
		return check.isTrue(columnValues);
	}

	private abstract class Check {
		abstract boolean isTrue(String[] line);
	}

}
