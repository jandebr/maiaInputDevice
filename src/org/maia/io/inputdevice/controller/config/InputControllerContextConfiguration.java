package org.maia.io.inputdevice.controller.config;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class InputControllerContextConfiguration {

	private String contextIdentifier;

	private List<InputCommandConfiguration> commandConfigurations;

	public InputControllerContextConfiguration(String contextIdentifier) {
		this.contextIdentifier = contextIdentifier;
		this.commandConfigurations = new Vector<InputCommandConfiguration>();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getContextIdentifier());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InputControllerContextConfiguration other = (InputControllerContextConfiguration) obj;
		return Objects.equals(getContextIdentifier(), other.getContextIdentifier());
	}

	public void addCommandConfiguration(InputCommandConfiguration commandConfiguration) {
		getCommandConfigurations().add(commandConfiguration);
	}

	public String getContextIdentifier() {
		return contextIdentifier;
	}

	public List<InputCommandConfiguration> getCommandConfigurations() {
		return commandConfigurations;
	}

}