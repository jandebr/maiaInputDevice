package org.maia.io.inputdevice;

import java.util.List;
import java.util.Objects;

public abstract class InputDevice {

	private String name;

	private String identifier;

	private List<Input> inputs;

	protected InputDevice(String name, String identifier) {
		this.name = name;
		this.identifier = identifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getIdentifier());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InputDevice other = (InputDevice) obj;
		return Objects.equals(getIdentifier(), other.getIdentifier());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InputDevice [name=");
		builder.append(getName());
		builder.append(", identifier=");
		builder.append(getIdentifier());
		builder.append(", numberOfInputs=");
		builder.append(getInputs().size());
		builder.append("]");
		return builder.toString();
	}

	public abstract String getTypeString();

	public abstract boolean isTypeMouse();

	public abstract boolean isTypeKeyboard();

	public abstract boolean isTypeStick();

	public abstract boolean isTypeGamepad();

	public abstract boolean isTypeUnknown();

	protected abstract List<Input> enumerateInputs();

	public String getName() {
		return name;
	}

	public String getIdentifier() {
		return identifier;
	}

	public List<Input> getInputs() {
		if (inputs == null) {
			inputs = enumerateInputs();
		}
		return inputs;
	}

	public Input getInputWithIdentifier(String identifier) {
		for (Input input : getInputs()) {
			if (input.getIdentifier().equals(identifier))
				return input;
		}
		return null;
	}

}