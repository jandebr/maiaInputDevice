package org.maia.io.inputdevice.controller.config;

import org.maia.io.inputdevice.controller.GatedInputController;
import org.maia.io.inputdevice.controller.GatedInputSelector;
import org.maia.io.inputdevice.controller.InputCommand;
import org.maia.io.inputdevice.controller.InputController;
import org.maia.io.inputdevice.controller.InputControllerContext;
import org.maia.io.inputdevice.controller.InputControllerException;
import org.maia.io.inputdevice.controller.InputControllerType;
import org.maia.io.inputdevice.controller.InputSelector;
import org.maia.io.inputdevice.joystick.Joystick;
import org.maia.io.inputdevice.joystick.JoystickContext;

public class InputControllerByConfigurationBuilder extends InputControllerBuilder {

	private InputControllerConfiguration configuration;

	public InputControllerByConfigurationBuilder(InputControllerConfiguration configuration) {
		if (configuration == null)
			throw new NullPointerException("Configuration is null");
		this.configuration = configuration;
	}

	@Override
	public InputController build() throws InputControllerException {
		return buildController(getConfiguration());
	}

	public GatedInputController buildGated() throws InputControllerException {
		try {
			return (GatedInputController) build();
		} catch (ClassCastException e) {
			throw new InputControllerException("Incompatible type: " + getConfiguration().getControllerType().name());
		}
	}

	public Joystick buildJoystick() throws InputControllerException {
		try {
			return (Joystick) build();
		} catch (ClassCastException e) {
			throw new InputControllerException("Incompatible type: " + getConfiguration().getControllerType().name());
		}
	}

	private InputController buildController(InputControllerConfiguration configuration)
			throws InputControllerException {
		InputController controller = createController(configuration);
		InputControllerType controllerType = controller.getType();
		configureController(controller, configuration);
		for (InputControllerContextConfiguration contextConfiguration : configuration.getContextConfigurations()) {
			controller.addAndSwitchContext(createControllerContext(contextConfiguration, controllerType));
			for (InputCommandConfiguration commandConfiguration : contextConfiguration.getCommandConfigurations()) {
				InputCommand command = commandConfiguration.getInputCommand();
				InputSelector inputSelector = commandConfiguration.getInputSelector();
				if (inputSelector != null) {
					if (inputSelector instanceof GatedInputSelector) {
						try {
							GatedInputController gatedController = (GatedInputController) controller;
							gatedController.setupGatedCommand(command, (GatedInputSelector) inputSelector);
						} catch (ClassCastException e) {
							throw new InputControllerException("Gated commands require a gated controller: " + command);
						}
					} else {
						controller.setupCommand(command, inputSelector.getInputIdentifier());
					}
				}
			}
		}
		return controller;
	}

	private InputController createController(InputControllerConfiguration configuration)
			throws InputControllerException {
		InputController controller = null;
		String name = configuration.getControllerName();
		String deviceIdentifier = configuration.getDeviceIdentifier();
		InputControllerType controllerType = configuration.getControllerType();
		if (InputControllerType.JOYSTICK.equals(controllerType)) {
			controller = new Joystick(name, deviceIdentifier);
		} else if (InputControllerType.GATED.equals(controllerType)) {
			controller = new GatedInputController(name, deviceIdentifier);
		} else {
			controller = new InputController(name, deviceIdentifier);
		}
		return controller;
	}

	private void configureController(InputController controller, InputControllerConfiguration configuration) {
		if (controller instanceof GatedInputController) {
			GatedInputController gatedController = (GatedInputController) controller;
			gatedController.setConcurrentlyFiringCommandsEnabled(configuration.isConcurrentlyFiringCommandsEnabled());
			gatedController.setFastReleasing(configuration.isFastReleasing());
		}
	}

	private InputControllerContext createControllerContext(InputControllerContextConfiguration contextConfiguration,
			InputControllerType controllerType) {
		String identifier = contextConfiguration.getContextIdentifier();
		if (InputControllerType.JOYSTICK.equals(controllerType)) {
			return new JoystickContext(identifier);
		} else {
			return new InputControllerContext(identifier);
		}
	}

	private InputControllerConfiguration getConfiguration() {
		return configuration;
	}

}