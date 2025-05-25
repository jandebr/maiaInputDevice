package org.maia.io.inputdevice.controller;

import org.maia.util.GenericListener;

public interface InputControllerListener extends GenericListener {

	void inputCommandValueChanged(InputController controller, InputCommand command, float value);

}