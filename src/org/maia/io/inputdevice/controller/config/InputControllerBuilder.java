package org.maia.io.inputdevice.controller.config;

import org.maia.io.inputdevice.controller.InputController;
import org.maia.io.inputdevice.controller.InputControllerException;

public abstract class InputControllerBuilder {

	protected InputControllerBuilder() {
	}

	public abstract InputController build() throws InputControllerException;

}