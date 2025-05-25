package org.maia.io.inputdevice.controller;

import java.util.Objects;

import org.maia.io.inputdevice.InputValueRange;

public class GatedInputSelector extends InputSelector {

	private InputValueRange inputFiringRange;

	public GatedInputSelector(String deviceIdentifier, String inputIdentifier, String inputName,
			InputValueRange inputFiringRange) {
		super(deviceIdentifier, inputIdentifier, inputName);
		if (inputFiringRange == null)
			throw new NullPointerException("Unspecified input firing range");
		this.inputFiringRange = inputFiringRange;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDeviceIdentifier(), getInputIdentifier(), getInputFiringRange());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof GatedInputSelector))
			return false;
		GatedInputSelector other = (GatedInputSelector) obj;
		return Objects.equals(getDeviceIdentifier(), other.getDeviceIdentifier())
				&& Objects.equals(getInputIdentifier(), other.getInputIdentifier())
				&& Objects.equals(getInputFiringRange(), other.getInputFiringRange());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getInputName());
		sb.append(" (").append(getInputIdentifier()).append(") ");
		sb.append(getInputFiringRange());
		return sb.toString();
	}

	public InputValueRange getInputFiringRange() {
		return inputFiringRange;
	}

}