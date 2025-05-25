package org.maia.io.inputdevice.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.maia.io.inputdevice.Input;
import org.maia.io.inputdevice.InputDevice;
import org.maia.io.inputdevice.InputEvent;
import org.maia.io.inputdevice.InputEventGateway;
import org.maia.io.inputdevice.InputEventListener;
import org.maia.util.GenericListenerList;
import org.maia.util.SystemUtils;

public class InputController implements InputEventListener {

	private InputControllerType type;

	private String name;

	private InputDevice device;

	private InputControllerContext currentContext;

	private Set<InputControllerContext> contexts;

	private GenericListenerList<InputControllerListener> listeners;

	private boolean active = true;

	private static InputDevice findInputDevice(String deviceIdentifier) throws InputControllerException {
		InputDevice device = InputEventGateway.getInstance().getInputDeviceWithIdentifier(deviceIdentifier);
		if (device == null) {
			throw new InputControllerException("Could not find device with identifier '" + deviceIdentifier + "'");
		} else {
			return device;
		}
	}

	public InputController(String name, String deviceIdentifier) throws InputControllerException {
		this(name, deviceIdentifier, null);
	}

	public InputController(String name, String deviceIdentifier, InputControllerContext initialContext)
			throws InputControllerException {
		this(name, findInputDevice(deviceIdentifier), initialContext);
	}

	public InputController(String name, InputDevice device) {
		this(name, device, null);
	}

	public InputController(String name, InputDevice device, InputControllerContext initialContext) {
		if (name == null || name.isEmpty())
			throw new IllegalArgumentException("Must specify a non-empty controller name");
		this.type = InputControllerType.OTHER;
		this.name = name;
		this.device = device;
		this.contexts = new HashSet<InputControllerContext>();
		this.listeners = new GenericListenerList<InputControllerListener>();
		if (initialContext != null)
			addAndSwitchContext(initialContext);
		init();
	}

	protected void init() {
		subscribeToInputEvents();
	}

	private void subscribeToInputEvents() {
		InputEventGateway.getInstance().registerDeviceListener(getDevice(), this);
	}

	private void unsubscribeToInputEvents() {
		InputEventGateway.getInstance().unregisterDeviceListener(getDevice(), this);
	}

	public void deactivateDuring(long durationInMillis) {
		setActive(false);
		new Thread() {

			@Override
			public void run() {
				SystemUtils.sleep(durationInMillis);
				setActive(true);
			}

		}.start();
	}

	public synchronized void removeAllContexts() {
		getContexts().clear();
		setCurrentContext(null);
	}

	public synchronized void addAndSwitchContext(InputControllerContext context) {
		addContext(context);
		switchContext(context);
	}

	public synchronized void addContext(InputControllerContext context) {
		if (context == null)
			throw new NullPointerException("Added context cannot be null");
		getContexts().add(context);
	}

	public synchronized void switchContext(String contextIdentifier) throws InputControllerException {
		InputControllerContext context = getContextWithIdentifier(contextIdentifier);
		if (context == null)
			throw new InputControllerException("No context was added with identifier '" + contextIdentifier + "'");
		switchContext(context);
	}

	private void switchContext(InputControllerContext context) {
		if (hasCurrentContext() && getCurrentContext().equals(context))
			return;
		if (hasCurrentContext()) {
			getCurrentContext().exit(this);
		}
		setCurrentContext(context);
		if (context != null) {
			context.init(this);
		}
	}

	private InputControllerContext createNewContext() {
		String newContextIdentifier = getDevice().getIdentifier() + "-context-" + System.currentTimeMillis();
		return new InputControllerContext(newContextIdentifier);
	}

	public synchronized void setupCommand(InputCommand command, String inputIdentifier)
			throws InputControllerException {
		setupCommand(new InputCommandProducer(command), inputIdentifier);
	}

	public synchronized void setupCommand(InputCommandProducer commandProducer, String inputIdentifier)
			throws InputControllerException {
		String deviceIdentifier = getDevice().getIdentifier();
		Input input = InputEventGateway.getInstance().getInputWithIdentifier(deviceIdentifier, inputIdentifier);
		if (input != null) {
			setupCommand(commandProducer, input);
		} else {
			throw new InputControllerException("Could not find input with identifier '" + inputIdentifier + "'");
		}
	}

	public synchronized void setupCommand(InputCommandProducer commandProducer, Input input) {
		if (!hasCurrentContext()) {
			addContext(createNewContext());
		}
		getCurrentContext().registerInputProcessor(input, commandProducer);
	}

	@Override
	public void receiveInputEvent(InputEvent event) {
		InputControllerContext context = getCurrentContext();
		if (context != null) {
			context.process(event, this);
		}
	}

	public void addListener(InputControllerListener listener) {
		getListeners().addListener(listener);
	}

	public void removeListener(InputControllerListener listener) {
		getListeners().removeListener(listener);
	}

	protected void fireInputCommandValueChanged(InputCommand command, float value) {
		if (isActive()) {
			for (InputControllerListener listener : getListeners()) {
				listener.inputCommandValueChanged(this, command, value);
			}
		}
	}

	public void dispose() {
		unsubscribeToInputEvents();
		if (hasCurrentContext()) {
			getCurrentContext().exit(this);
		}
	}

	@Override
	protected void finalize() {
		dispose();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InputController [name=");
		builder.append(getName());
		builder.append(", contexts=");
		builder.append(getContextIdentifiers());
		builder.append(", currentContext=");
		builder.append(getCurrentContextIdentifier());
		builder.append("]");
		return builder.toString();
	}

	public synchronized InputControllerContext getContextWithIdentifier(String contextIdentifier) {
		for (InputControllerContext context : getContexts()) {
			if (context.getIdentifier().equals(contextIdentifier))
				return context;
		}
		return null;
	}

	public synchronized Set<String> getContextIdentifiers() {
		Set<String> identifiers = new HashSet<String>();
		for (InputControllerContext context : getContexts()) {
			identifiers.add(context.getIdentifier());
		}
		return identifiers;
	}

	public boolean hasCurrentContext() {
		return getCurrentContext() != null;
	}

	public String getCurrentContextIdentifier() {
		InputControllerContext context = getCurrentContext();
		return context != null ? context.getIdentifier() : null;
	}

	public Set<InputCommand> getCurrentContextCommands() {
		InputControllerContext context = getCurrentContext();
		if (context != null) {
			return context.getInputProcessorCommands();
		} else {
			return Collections.emptySet();
		}
	}

	public InputControllerType getType() {
		return type;
	}

	protected void setType(InputControllerType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public InputDevice getDevice() {
		return device;
	}

	public InputControllerContext getCurrentContext() {
		return currentContext;
	}

	private void setCurrentContext(InputControllerContext currentContext) {
		this.currentContext = currentContext;
	}

	private Set<InputControllerContext> getContexts() {
		return contexts;
	}

	private GenericListenerList<InputControllerListener> getListeners() {
		return listeners;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}