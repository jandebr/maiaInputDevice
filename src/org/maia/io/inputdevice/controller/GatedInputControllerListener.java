package org.maia.io.inputdevice.controller;

import org.maia.util.GenericListener;

public interface GatedInputControllerListener extends GenericListener {

	void inputCommandFired(GatedInputController controller, InputCommand command, boolean autoRepeat);

	void inputCommandReleased(GatedInputController controller, InputCommand command);

}