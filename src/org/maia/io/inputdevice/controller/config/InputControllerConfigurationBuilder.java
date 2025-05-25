package org.maia.io.inputdevice.controller.config;

import org.maia.io.inputdevice.controller.InputControllerType;

public abstract class InputControllerConfigurationBuilder {

	protected InputControllerConfigurationBuilder() {
	}

	public abstract InputControllerConfiguration build();

	protected InputControllerType getDefaultInputControllerType() {
		return InputControllerType.OTHER;
	}

}