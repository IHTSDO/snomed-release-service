package org.ihtsdo.buildcloud.core.service.build.transform.conditional;

import org.ihtsdo.buildcloud.core.service.build.transform.LineTransformation;
import org.ihtsdo.buildcloud.core.service.build.transform.TransformationException;

import java.util.ArrayList;
import java.util.List;

public class ConditionalTransformation implements LineTransformation {

	private final List<Condition> conditions;
	private LineTransformation conditionsTrueTransformation;
	private LineTransformation conditionsFalseTransformation;

	public ConditionalTransformation() {
		conditions = new ArrayList<>();
	}

	@Override
	public void transformLine(String[] columnValues) throws TransformationException {
		boolean allConditionsPass = true;
		for (Condition condition : conditions) {
			if (!condition.test(columnValues)) {
				allConditionsPass = false;
				break;
			}
		}

		if (allConditionsPass) {
			if (conditionsTrueTransformation != null) {
				conditionsTrueTransformation.transformLine(columnValues);
			}
		} else {
			if (conditionsFalseTransformation != null) {
				conditionsFalseTransformation.transformLine(columnValues);
			}
		}
	}

	@Override
	public int getColumnIndex() {
		return -1;
	}

	public Condition addIf() {
		Condition condition = new Condition(this);
		conditions.add(condition);
		return condition;
	}

	public Condition and() {
		return addIf();
	}

	public ConditionalTransformation then(LineTransformation transformation) {
		this.conditionsTrueTransformation = transformation;
		return this;
	}

	public ConditionalTransformation otherwise(LineTransformation transformation) {
		this.conditionsFalseTransformation = transformation;
		return this;
	}

}
