package org.maia.io.inputdevice;

public interface AnalogInputSensitive {

	float MINIMUM_SENSITIVITY_INCLUSIVE = 0f;

	float MAXIMUM_SENSITIVITY_EXCLUSIVE = 1.0f;

	float getAnalogSensitivity();

	void changeAnalogSensitivity(float sensitivity);

}