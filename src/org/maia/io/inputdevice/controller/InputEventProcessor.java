package org.maia.io.inputdevice.controller;

import org.maia.io.inputdevice.InputEvent;

public interface InputEventProcessor {

	void init(InputController controller);

	void process(InputEvent event, InputController controller);

	void exit(InputController controller);

}