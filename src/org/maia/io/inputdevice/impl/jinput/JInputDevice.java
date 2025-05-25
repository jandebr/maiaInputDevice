package org.maia.io.inputdevice.impl.jinput;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.maia.io.inputdevice.Input;
import org.maia.io.inputdevice.InputDevice;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;

public class JInputDevice extends InputDevice {

	private Controller controller;

	public JInputDevice(Controller controller) {
		super(controller.getName(), deriveIdentifier(controller));
		this.controller = controller;
	}

	private static String deriveIdentifier(Controller controller) {
		// Fingerprint hash from type, name and sorted component ids
		StringBuilder id = new StringBuilder(256);
		id.append(controller.getType().toString());
		id.append('|');
		id.append(controller.getName());
		for (Component component : getComponentsSortedByIdentifier(controller)) {
			id.append('|');
			id.append(component.getIdentifier().toString());
		}
		return "JInputDevice-" + Math.abs(id.toString().hashCode());
	}

	private static List<Component> getComponentsSortedByIdentifier(Controller controller) {
		List<Component> components = Arrays.asList(controller.getComponents());
		Collections.sort(components, new Comparator<Component>() {

			@Override
			public int compare(Component c1, Component c2) {
				return c1.getIdentifier().toString().compareTo(c2.getIdentifier().toString());
			}

		});
		return components;
	}

	@Override
	protected List<Input> enumerateInputs() {
		Component[] components = getController().getComponents();
		List<Input> inputs = new Vector<Input>(components.length);
		for (int i = 0; i < components.length; i++) {
			inputs.add(new JInput(this, components[i]));
		}
		return inputs;
	}

	@Override
	public String getTypeString() {
		return getController().getType().toString();
	}

	@Override
	public boolean isTypeMouse() {
		return isType(Type.MOUSE);
	}

	@Override
	public boolean isTypeKeyboard() {
		return isType(Type.KEYBOARD);
	}

	@Override
	public boolean isTypeStick() {
		return isType(Type.STICK);
	}

	@Override
	public boolean isTypeGamepad() {
		return isType(Type.GAMEPAD);
	}

	@Override
	public boolean isTypeUnknown() {
		return isType(Type.UNKNOWN);
	}

	private boolean isType(Type type) {
		return type.equals(getController().getType());
	}

	private Controller getController() {
		return controller;
	}

}