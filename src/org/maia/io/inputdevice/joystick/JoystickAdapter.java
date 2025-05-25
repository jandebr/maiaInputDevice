package org.maia.io.inputdevice.joystick;

public abstract class JoystickAdapter implements JoystickListener {

	protected JoystickAdapter() {
	}

	@Override
	public void joystickCommandFired(Joystick joystick, JoystickCommand command, boolean autoRepeat) {
		// Subclasses can override this
	}

	@Override
	public void joystickCommandReleased(Joystick joystick, JoystickCommand command) {
		// Subclasses can override this
	}

}