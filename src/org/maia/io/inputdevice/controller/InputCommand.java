package org.maia.io.inputdevice.controller;

import java.util.Objects;

public class InputCommand {

	private String name;

	private String identifier;

	private String description;

	public InputCommand(String identifier) {
		this(identifier, identifier);
	}

	public InputCommand(String name, String identifier) {
		if (name == null || name.isEmpty())
			throw new IllegalArgumentException("Must specify a non-empty command name");
		if (identifier == null || identifier.isEmpty())
			throw new IllegalArgumentException("Must specify a non-empty command identifier");
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
		if (!(obj instanceof InputCommand))
			return false;
		InputCommand other = (InputCommand) obj;
		return Objects.equals(getIdentifier(), other.getIdentifier());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InputCommand [name=");
		builder.append(getName());
		builder.append(", identifier=");
		builder.append(getIdentifier());
		if (hasDescription()) {
			builder.append(", description=");
			builder.append(getDescription());
		}
		builder.append("]");
		return builder.toString();
	}

	public String getName() {
		return name;
	}

	public String getIdentifier() {
		return identifier;
	}

	public boolean hasDescription() {
		return getDescription() != null && !getDescription().isEmpty();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}