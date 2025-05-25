package org.maia.io.inputdevice.joystick;

public interface JoystickListener {

	void joystickCommandFired(Joystick joystick, JoystickCommand command, boolean autoRepeat);

	void joystickCommandReleased(Joystick joystick, JoystickCommand command);

}