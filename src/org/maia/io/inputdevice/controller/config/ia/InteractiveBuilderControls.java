package org.maia.io.inputdevice.controller.config.ia;

import org.maia.io.inputdevice.controller.InputCommand;

public interface InteractiveBuilderControls {

	InteractiveBuilderControlType getType(InputCommand command);

}