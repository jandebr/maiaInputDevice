package org.maia.io.inputdevice;

import java.util.List;

public abstract class InputEventSource {

	private InputDeviceFilter deviceFilter;

	private InputFilter inputFilter;

	protected InputEventSource(InputDeviceFilter deviceFilter, InputFilter inputFilter) {
		this.deviceFilter = deviceFilter;
		this.inputFilter = inputFilter;
	}

	public abstract InputFilter createExplicitUserGestureInputFilter();

	public abstract List<InputEvent> pollEvents();

	public abstract void scanInputDevices();

	public abstract List<InputDevice> getInputDevices();

	public InputDevice getInputDeviceWithIdentifier(String identifier) {
		for (InputDevice device : getInputDevices()) {
			if (device.getIdentifier().equals(identifier))
				return device;
		}
		return null;
	}

	public InputDeviceFilter getDeviceFilter() {
		return deviceFilter;
	}

	public void setDeviceFilter(InputDeviceFilter deviceFilter) {
		this.deviceFilter = deviceFilter;
	}

	public InputFilter getInputFilter() {
		return inputFilter;
	}

	public void setInputFilter(InputFilter inputFilter) {
		this.inputFilter = inputFilter;
	}

}