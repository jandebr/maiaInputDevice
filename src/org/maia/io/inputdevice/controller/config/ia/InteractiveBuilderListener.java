package org.maia.io.inputdevice.controller.config.ia;

import org.maia.io.inputdevice.controller.InputCommand;
import org.maia.util.GenericListener;

public interface InteractiveBuilderListener extends GenericListener {

	void notifyChangeOfCurrentCommand(InteractiveBuilder builder);

	void notifyDeviceAssignment(InteractiveBuilder builder, String deviceIdentifier);

	void notifyCommandAssignment(InteractiveBuilder builder, InputCommand command);

	void notifyChangeOfCommandAssignments(InteractiveBuilder builder);

	void notifySubmitCommandAssignments(InteractiveBuilder builder);

}