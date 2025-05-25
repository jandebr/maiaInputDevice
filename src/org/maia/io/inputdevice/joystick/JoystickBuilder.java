package org.maia.io.inputdevice.joystick;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.maia.io.inputdevice.controller.InputControllerException;
import org.maia.io.inputdevice.controller.config.InputControllerByConfigurationBuilder;
import org.maia.io.inputdevice.controller.config.InputControllerConfiguration;
import org.maia.io.inputdevice.controller.config.InputControllerConfigurationByPropertiesBuilder;

public class JoystickBuilder {

	public JoystickBuilder() {
	}

	public Joystick buildFromConfigurationFile(File cfgFile, boolean autoRepeatEnabled)
			throws IOException, InputControllerException {
		Properties props = InputControllerConfigurationByPropertiesBuilder.readPropertiesFromFile(cfgFile);
		InputControllerConfiguration cfg = new InputControllerConfigurationByPropertiesBuilder(props).build();
		return buildFromConfiguration(cfg, autoRepeatEnabled);
	}

	public Joystick buildFromConfiguration(InputControllerConfiguration cfg, boolean autoRepeatEnabled)
			throws InputControllerException {
		Joystick joystick = new InputControllerByConfigurationBuilder(cfg).buildJoystick();
		joystick.setAutoRepeatEnabled(autoRepeatEnabled);
		return joystick;
	}

}