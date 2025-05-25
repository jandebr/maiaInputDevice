package org.maia.io.inputdevice.controller.config.ia;

import org.maia.io.inputdevice.controller.InputCommand;

public interface RequiredInputCommands {

	RequiredInputCommands REQUIRE_ALL = new RequiredInputCommands() {

		@Override
		public boolean isRequired(InputCommand command) {
			return true;
		}
	};

	RequiredInputCommands REQUIRE_NONE = new RequiredInputCommands() {

		@Override
		public boolean isRequired(InputCommand command) {
			return false;
		}
	};

	boolean isRequired(InputCommand command);

}