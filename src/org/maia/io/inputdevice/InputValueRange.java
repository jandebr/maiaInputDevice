package org.maia.io.inputdevice;

import java.util.Objects;

public class InputValueRange implements InputValueFilter {

	private float minimumValue;

	private float maximumValue;

	public InputValueRange(float singleValue) {
		this(singleValue, singleValue);
	}

	public InputValueRange(float minimumValue, float maximumValue) {
		setRange(minimumValue, maximumValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMinimumValue(), getMaximumValue());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof InputValueRange))
			return false;
		InputValueRange other = (InputValueRange) obj;
		return getMinimumValue() == other.getMinimumValue() && getMaximumValue() == other.getMaximumValue();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(getMinimumValue());
		sb.append(",");
		sb.append(getMaximumValue());
		sb.append("]");
		return sb.toString();
	}

	protected void setRange(float minimumValue, float maximumValue) {
		if (minimumValue > maximumValue)
			throw new IllegalArgumentException(
					"Minimum cannot be greater than maximum: " + minimumValue + ", " + maximumValue);
		this.minimumValue = minimumValue;
		this.maximumValue = maximumValue;
	}

	public boolean isSingleValue() {
		return getMinimumValue() == getMaximumValue();
	}

	@Override
	public boolean accept(float value) {
		return contains(value);
	}

	public boolean contains(float value) {
		return value >= getMinimumValue() && value <= getMaximumValue();
	}

	public float getMinimumValue() {
		return minimumValue;
	}

	public float getMaximumValue() {
		return maximumValue;
	}

}