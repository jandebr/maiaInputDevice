package org.maia.io.inputdevice.joystick;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.maia.io.inputdevice.Test;
import org.maia.io.inputdevice.controller.InputControllerException;
import org.maia.io.inputdevice.controller.config.InputControllerByConfigurationBuilder;
import org.maia.io.inputdevice.controller.config.InputControllerConfiguration;
import org.maia.io.inputdevice.controller.config.InputControllerConfigurationByPropertiesBuilder;

public class JoystickTest extends Test implements JoystickListener {

	private static final String PLAYSTATION_CONTROLLER_ID = "JInputDevice-718747082";

	private static final String PLAYSTATION_POV_ID = "pov";

	private static final String PLAYSTATION_X_ID = "x";

	private static final String PLAYSTATION_Y_ID = "y";

	private static final String PLAYSTATION_BUTTON1_ID = "1";

	private static final String CONTEXT_XY = "xy";

	private static final String CONTEXT_POV = "pov";

	private static final JoystickCommand COMMAND_LEFT = new JoystickCommand("Left");

	private static final JoystickCommand COMMAND_RIGHT = new JoystickCommand("Right");

	private static final JoystickCommand COMMAND_UP = new JoystickCommand("Up");

	private static final JoystickCommand COMMAND_DOWN = new JoystickCommand("Down");

	private static final JoystickCommand COMMAND_SELECT = new JoystickCommand("Select");

	public static void main(String[] args) throws Exception {
		new JoystickTest().startTest();
	}

	@Override
	public void startTest() throws Exception {
		showFrame("Joystick test");
		Joystick joystick = createJoystickFromProperties(new File("testcontroller.config"), "testcontroller");
		// Joystick joystick = createJoystickProgrammatically();
		// joystick.switchContext(CONTEXT_POV);
		joystick.setAutoRepeatEnabled(false);
		joystick.addJoystickListener(this);
		System.out.println(joystick);
		System.out.println(joystick.getCurrentContextCommands());
	}

	private Joystick createJoystickFromProperties(File propertiesFile, String keyPrefix)
			throws InputControllerException, IOException {
		Properties properties = InputControllerConfigurationByPropertiesBuilder.readPropertiesFromFile(propertiesFile);
		InputControllerConfiguration configuration = new InputControllerConfigurationByPropertiesBuilder(properties,
				keyPrefix).build();
		return new InputControllerByConfigurationBuilder(configuration).buildJoystick();
	}

	private Joystick createJoystickProgrammatically() throws InputControllerException {
		Joystick joystick = new Joystick("Joystick-1", PLAYSTATION_CONTROLLER_ID);
		setupXYJoystickContext(joystick);
		setupPovJoystickContext(joystick);
		return joystick;
	}

	private void setupXYJoystickContext(Joystick joystick) throws InputControllerException {
		joystick.addAndSwitchContext(new JoystickContext(CONTEXT_XY));
		joystick.addCommand(COMMAND_LEFT, PLAYSTATION_X_ID, -1.0f, -0.5f);
		joystick.addCommand(COMMAND_RIGHT, PLAYSTATION_X_ID, 0.5f, 1.0f);
		joystick.addCommand(COMMAND_UP, PLAYSTATION_Y_ID, -1.0f, -0.5f);
		joystick.addCommand(COMMAND_DOWN, PLAYSTATION_Y_ID, 0.5f, 1.0f);
		joystick.addCommand(COMMAND_SELECT, PLAYSTATION_BUTTON1_ID);
	}

	private void setupPovJoystickContext(Joystick joystick) throws InputControllerException {
		joystick.addAndSwitchContext(new JoystickContext(CONTEXT_POV));
		joystick.addCommand(COMMAND_LEFT, PLAYSTATION_POV_ID, 1.0f);
		joystick.addCommand(COMMAND_DOWN, PLAYSTATION_POV_ID, 0.75f);
		joystick.addCommand(COMMAND_RIGHT, PLAYSTATION_POV_ID, 0.5f);
		joystick.addCommand(COMMAND_UP, PLAYSTATION_POV_ID, 0.25f);
		joystick.addCommand(COMMAND_SELECT, PLAYSTATION_BUTTON1_ID);
	}

	@Override
	public void joystickCommandFired(Joystick joystick, JoystickCommand command, boolean autoRepeat) {
		System.out.println(
				joystick.getName() + " fired " + command.getIdentifier() + (autoRepeat ? " (auto-repeat)" : ""));
	}

	@Override
	public void joystickCommandReleased(Joystick joystick, JoystickCommand command) {
		System.out.println(joystick.getName() + " released " + command.getIdentifier());
		if (command.equals(COMMAND_SELECT)) {
			String contextId = joystick.getCurrentContextIdentifier();
			String newContextId = CONTEXT_XY.equals(contextId) ? CONTEXT_POV : CONTEXT_XY;
			System.out.println("Switching context to " + newContextId);
			try {
				joystick.switchContext(newContextId);
			} catch (InputControllerException e) {
				e.printStackTrace();
			}
		}
	}

}