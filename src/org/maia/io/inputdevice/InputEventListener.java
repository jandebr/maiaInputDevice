package org.maia.io.inputdevice;

import org.maia.util.GenericListener;

public interface InputEventListener extends GenericListener {

	void receiveInputEvent(InputEvent event);

}