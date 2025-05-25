package org.maia.io.inputdevice.joystick;

import org.maia.io.inputdevice.InputDevice;
import org.maia.io.inputdevice.InputValueRange;
import org.maia.io.inputdevice.controller.GatedInputController;
import org.maia.io.inputdevice.controller.GatedInputControllerListener;
import org.maia.io.inputdevice.controller.GatedInputSelector;
import org.maia.io.inputdevice.controller.InputCommand;
import org.maia.io.inputdevice.controller.InputControllerException;
import org.maia.io.inputdevice.controller.InputControllerType;

public class Joystick extends GatedInputController {

	public Joystick(String name, String deviceIdentifier) throws InputControllerException {
		super(name, deviceIdentifier);
	}

	public Joystick(String name, String deviceIdentifier, JoystickContext initialContext)
			throws InputControllerException {
		super(name, deviceIdentifier, initialContext);
	}

	public Joystick(String name, InputDevice device) {
		super(name, device);
	}

	public Joystick(String name, InputDevice device, JoystickContext initialContext) {
		super(name, device, initialContext);
	}

	@Override
	protected void init() {
		super.init();
		setType(InputControllerType.JOYSTICK);
	}

	public synchronized void addJoystickListener(JoystickListener listener) {
		addGatedListener(new JoystickListenerAdapter(listener));
	}

	public synchronized void removeJoystickListener(JoystickListener listener) {
		JoystickListenerAdapter adapter = findRegisteredAdapterFor(listener);
		if (adapter != null) {
			removeGatedListener(adapter);
		}
	}

	public void addCommand(JoystickCommand command, String inputIdentifier) throws InputControllerException {
		setupGatedCommand(command, inputIdentifier);
	}

	public void addCommand(JoystickCommand command, String inputIdentifier, float inputFiringValue)
			throws InputControllerException {
		setupGatedCommand(command, inputIdentifier, inputFiringValue);
	}

	public void addCommand(JoystickCommand command, String inputIdentifier, float inputFiringMinValue,
			float inputFiringMaxValue) throws InputControllerException {
		setupGatedCommand(command, inputIdentifier, inputFiringMinValue, inputFiringMaxValue);
	}

	public void addCommand(JoystickCommand command, String inputIdentifier, InputValueRange inputFiringRange)
			throws InputControllerException {
		setupGatedCommand(command, inputIdentifier, inputFiringRange);
	}

	public void addCommand(JoystickCommand command, GatedInputSelector inputSelector) throws InputControllerException {
		setupGatedCommand(command, inputSelector);
	}

	private JoystickListenerAdapter findRegisteredAdapterFor(JoystickListener listener) {
		for (GatedInputControllerListener gatedListener : getGatedListeners()) {
			if (gatedListener instanceof JoystickListenerAdapter) {
				JoystickListenerAdapter adapter = (JoystickListenerAdapter) gatedListener;
				if (adapter.getListener().equals(listener)) {
					return adapter;
				}
			}
		}
		return null;
	}

	private class JoystickListenerAdapter implements GatedInputControllerListener {

		private JoystickListener listener;

		public JoystickListenerAdapter(JoystickListener listener) {
			this.listener = listener;
		}

		@Override
		public void inputCommandFired(GatedInputController controller, InputCommand command, boolean autoRepeat) {
			getListener().joystickCommandFired(Joystick.this, asJoystickCommand(command), autoRepeat);
		}

		@Override
		public void inputCommandReleased(GatedInputController controller, InputCommand command) {
			getListener().joystickCommandReleased(Joystick.this, asJoystickCommand(command));
		}

		private JoystickCommand asJoystickCommand(InputCommand command) {
			if (command instanceof JoystickCommand) {
				return (JoystickCommand) command;
			} else {
				return new JoystickCommand(command.getName(), command.getIdentifier());
			}
		}

		public JoystickListener getListener() {
			return listener;
		}

	}

}