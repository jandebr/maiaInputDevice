package org.maia.io.inputdevice.controller.config;

import java.util.Objects;

import org.maia.io.inputdevice.controller.InputCommand;
import org.maia.io.inputdevice.controller.InputSelector;

public class InputCommandConfiguration {

	private InputCommand inputCommand;

	private InputSelector inputSelector;

	public InputCommandConfiguration(InputCommand inputCommand, InputSelector inputSelector) {
		this.inputCommand = inputCommand;
		this.inputSelector = inputSelector;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getInputCommand(), getInputSelector());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InputCommandConfiguration other = (InputCommandConfiguration) obj;
		return Objects.equals(getInputCommand(), other.getInputCommand())
				&& Objects.equals(getInputSelector(), other.getInputSelector());
	}

	public InputCommand getInputCommand() {
		return inputCommand;
	}

	public InputSelector getInputSelector() {
		return inputSelector;
	}

}