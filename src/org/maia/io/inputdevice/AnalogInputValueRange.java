package org.maia.io.inputdevice;

public class AnalogInputValueRange extends InputValueRange implements AnalogInputSensitive {

	public AnalogInputValueRange(float minimumValue, float maximumValue) {
		super(minimumValue, maximumValue);
	}

	@Override
	public float getAnalogSensitivity() {
		if (hasPositiveDomain()) {
			return 1.0f - getMinimumValue();
		} else {
			return 1.0f + getMaximumValue();
		}
	}

	@Override
	public void changeAnalogSensitivity(float sensitivity) {
		if (sensitivity < MINIMUM_SENSITIVITY_INCLUSIVE || sensitivity >= MAXIMUM_SENSITIVITY_EXCLUSIVE)
			throw new IllegalArgumentException("Sensitivity value out of range: " + sensitivity);
		float s = 1.0f - sensitivity;
		if (hasPositiveDomain()) {
			setRange(s, Math.max(getMaximumValue(), s));
		} else {
			setRange(Math.min(getMinimumValue(), -s), -s);
		}
	}

	private boolean hasPositiveDomain() {
		return getMinimumValue() >= 0f;
	}

}