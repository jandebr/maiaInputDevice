package org.maia.io.inputdevice.controller.config.ia;

import org.maia.util.GenericListener;

public interface JInteractiveBuilderListener extends GenericListener {

	void interactiveBuilderCompleted(JInteractiveBuilder jbuilder);

	void interactiveBuilderCancelled(JInteractiveBuilder jbuilder);

}