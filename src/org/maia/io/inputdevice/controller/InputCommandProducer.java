package org.maia.io.inputdevice.controller;

import org.maia.io.inputdevice.InputEvent;

public class InputCommandProducer implements InputEventProcessor {

	private InputCommand command;

	public InputCommandProducer(InputCommand command) {
		this.command = command;
	}

	@Override
	public void init(InputController controller) {
		// Subclasses can override this method
	}

	@Override
	public void process(InputEvent event, InputController controller) {
		controller.fireInputCommandValueChanged(getCommand(), event.getValue());
	}

	@Override
	public void exit(InputController controller) {
		// Subclasses can override this method
	}

	public InputCommand getCommand() {
		return command;
	}

}