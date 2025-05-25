package org.maia.io.inputdevice.controller;

import java.util.Objects;

import org.maia.io.inputdevice.Input;
import org.maia.io.inputdevice.InputEventGateway;

public class InputSelector {

	private String deviceIdentifier;

	private String inputIdentifier;

	private String inputName;

	public InputSelector(String deviceIdentifier, String inputIdentifier, String inputName) {
		if (deviceIdentifier == null || deviceIdentifier.isEmpty())
			throw new IllegalArgumentException("Must specify a non-empty device identifier");
		if (inputIdentifier == null || inputIdentifier.isEmpty())
			throw new IllegalArgumentException("Must specify a non-empty input identifier");
		if (inputName == null || inputName.isEmpty())
			throw new IllegalArgumentException("Must specify a non-empty input name");
		this.deviceIdentifier = deviceIdentifier;
		this.inputIdentifier = inputIdentifier;
		this.inputName = inputName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDeviceIdentifier(), getInputIdentifier());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof InputSelector))
			return false;
		InputSelector other = (InputSelector) obj;
		return Objects.equals(getDeviceIdentifier(), other.getDeviceIdentifier())
				&& Objects.equals(getInputIdentifier(), other.getInputIdentifier());
	}

	public Input resolveInput() {
		return InputEventGateway.getInstance().getInputWithIdentifier(getDeviceIdentifier(), getInputIdentifier());
	}

	public String getDeviceIdentifier() {
		return deviceIdentifier;
	}

	public String getInputIdentifier() {
		return inputIdentifier;
	}

	public String getInputName() {
		return inputName;
	}

}