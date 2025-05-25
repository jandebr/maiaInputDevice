package org.maia.io.inputdevice.impl.jinput;

import org.maia.io.inputdevice.Input;
import org.maia.io.inputdevice.InputValueRange;

import net.java.games.input.Component;

public class JInput extends Input {

	private Component component;

	private float lastValue;

	public JInput(JInputDevice device, Component component) {
		super(device, component.getName(), component.getIdentifier().getName());
		this.component = component;
	}

	@Override
	public boolean isAnalog() {
		return getComponent().isAnalog();
	}

	@Override
	public boolean isRelative() {
		return getComponent().isRelative();
	}

	public float getDeadZone() {
		return getComponent().getDeadZone();
	}

	@Override
	public InputValueRange getValueRange() {
		// Assumptions in the absence of information
		if (isDigital()) {
			return new InputValueRange(0, 1.0f);
		} else {
			return new InputValueRange(-1.0f, 1.0f);
		}
	}

	private Component getComponent() {
		return component;
	}

	float getLastValue() {
		return lastValue;
	}

	void setLastValue(float value) {
		this.lastValue = value;
	}

}