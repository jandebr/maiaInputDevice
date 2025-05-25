package org.maia.io.inputdevice;

public interface InputDeviceFilter {

	InputDeviceFilter ACCEPT_ALL = new InputDeviceFilter() {

		@Override
		public boolean accept(InputDevice device) {
			return true;
		}
	};

	InputDeviceFilter MOUSE = new InputDeviceFilter() {

		@Override
		public boolean accept(InputDevice device) {
			return device.isTypeMouse();
		}
	};

	InputDeviceFilter KEYBOARD = new InputDeviceFilter() {

		@Override
		public boolean accept(InputDevice device) {
			return device.isTypeKeyboard();
		}
	};

	InputDeviceFilter STICK = new InputDeviceFilter() {

		@Override
		public boolean accept(InputDevice device) {
			return device.isTypeStick();
		}
	};

	InputDeviceFilter GAMEPAD = new InputDeviceFilter() {

		@Override
		public boolean accept(InputDevice device) {
			return device.isTypeGamepad();
		}
	};

	InputDeviceFilter STICK_OR_GAMEPAD = new InputDeviceFilter() {

		@Override
		public boolean accept(InputDevice device) {
			return device.isTypeStick() || device.isTypeGamepad();
		}
	};

	boolean accept(InputDevice device);

}