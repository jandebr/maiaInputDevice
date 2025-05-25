package org.maia.io.inputdevice.joystick;

import org.maia.io.inputdevice.controller.InputCommand;

public class JoystickCommand extends InputCommand {

	public JoystickCommand(String identifier) {
		super(identifier);
	}

	public JoystickCommand(String name, String identifier) {
		super(name, identifier);
	}

}