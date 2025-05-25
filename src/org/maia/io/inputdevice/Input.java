package org.maia.io.inputdevice;

import java.util.Objects;

public abstract class Input {

	private InputDevice device;

	private String name;

	private String identifier;

	protected Input(InputDevice device, String name, String identifier) {
		this.device = device;
		this.name = name;
		this.identifier = identifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getDevice(), getIdentifier());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Input other = (Input) obj;
		return Objects.equals(getDevice(), other.getDevice()) && Objects.equals(getIdentifier(), other.getIdentifier());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Input [name=");
		builder.append(getName());
		builder.append(", identifier=");
		builder.append(getIdentifier());
		builder.append(", ");
		builder.append(isDigital() ? "digital" : "analog");
		builder.append(", ");
		builder.append(isAbsolute() ? "absolute" : "relative");
		builder.append("]");
		return builder.toString();
	}

	public final boolean isDigital() {
		return !isAnalog();
	}

	public abstract boolean isAnalog();

	public final boolean isAbsolute() {
		return !isRelative();
	}

	public abstract boolean isRelative();

	public abstract InputValueRange getValueRange();

	public InputDevice getDevice() {
		return device;
	}

	public String getName() {
		return name;
	}

	public String getIdentifier() {
		return identifier;
	}

}