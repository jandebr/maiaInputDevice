package org.maia.io.inputdevice.impl.jinput;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.maia.io.inputdevice.Input;
import org.maia.io.inputdevice.InputDevice;
import org.maia.io.inputdevice.InputDeviceFilter;
import org.maia.io.inputdevice.InputEvent;
import org.maia.io.inputdevice.InputEventSource;
import org.maia.io.inputdevice.InputFilter;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.PublicDefaultControllerEnvironment;

public class JInputEventSource extends InputEventSource {

	private float overshootValueChange = 1.1f; // suppress sporadic value changes beyond this value

	private float minimumAnalogDeadZone = 0;

	private boolean exposeEventsInDeadzone; // when false, don't expose events in deadzone

	private boolean renewControllerEnvironmentForScan; // when false, keep the same CE throughout the JVM's lifetime

	private boolean reuseEventObjects; // when true, reuses event objects over poll cycles

	private List<InputEvent> events; // reusable collection of events

	private ReusableEventPool reusableEventPool; // collection of reusable events

	private Controller[] controllers;

	private Map<Controller, JInputDevice> deviceMap;

	private Map<Component, JInput> inputMap;

	private ControllerEnvironment controllerEnvironment;

	private long controllerEnvironmentCreationTime;

	private long controllerEnvironmentStartupMaskTime; // suppress any 'signal noise' during CE startup

	private static long DEFAULT_CE_STARTUP_MASK_TIME = 400L;

	public static final String SYSTEM_PROPERTY_RENEW_CE = "jinput.renewCE";

	public static final String SYSTEM_PROPERTY_REUSE_EVENT_OBJECTS = "jinput.reuseEventObjects";

	public JInputEventSource() {
		this(InputDeviceFilter.ACCEPT_ALL);
	}

	public JInputEventSource(InputDeviceFilter deviceFilter) {
		this(deviceFilter, InputFilter.ACCEPT_ALL);
	}

	public JInputEventSource(InputDeviceFilter deviceFilter, InputFilter inputFilter) {
		super(deviceFilter, inputFilter);
		this.renewControllerEnvironmentForScan = Boolean
				.parseBoolean(System.getProperty(SYSTEM_PROPERTY_RENEW_CE, "true"));
		this.reuseEventObjects = Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_REUSE_EVENT_OBJECTS, "true"));
		this.events = new Vector<InputEvent>();
		this.reusableEventPool = new ReusableEventPool();
		this.deviceMap = new HashMap<Controller, JInputDevice>();
		this.inputMap = new HashMap<Component, JInput>(100);
	}

	@Override
	public InputFilter createExplicitUserGestureInputFilter() {
		return AxisInputFilter.createExplicitUserGestureFilter();
	}

	@Override
	public synchronized List<InputEvent> pollEvents() {
		if (isReuseEventObjects())
			getReusableEventPool().recycle();
		List<InputEvent> events = getEvents();
		events.clear();
		pollEvents(events);
		return events;
	}

	private void pollEvents(List<InputEvent> events) {
		Controller[] controllers = getControllers();
		for (int i = 0; i < controllers.length; i++) {
			Controller controller = controllers[i];
			JInputDevice device = mapToDevice(controller);
			if (getDeviceFilter().accept(device)) {
				pollEvents(controller, events);
			}
		}
	}

	private void pollEvents(Controller controller, List<InputEvent> events) {
		if (controller.poll()) {
			if (!isMaskEvents()) {
				Component[] components = controller.getComponents();
				for (int i = 0; i < components.length; i++) {
					Component component = components[i];
					pollEvents(controller, component, events);
				}
			}
		}
	}

	private void pollEvents(Controller controller, Component component, List<InputEvent> events) {
		JInput input = mapToInput(controller, component);
		if (getInputFilter().accept(input)) {
			float value = component.getPollData();
			float previousValue = input.getLastValue();
			boolean valueOfInterest = false;
			if (input.isDigital()) {
				valueOfInterest = value != previousValue;
			} else if (input.isAnalog()) {
				float valueDiff = Math.abs(value - previousValue);
				if (valueDiff > 0f && valueDiff <= getOvershootValueChange()) {
					float deadZone = Math.max(component.getDeadZone(), getMinimumAnalogDeadZone());
					if (isExposeEventsInDeadzone() || Math.abs(value) > deadZone
							|| Math.abs(previousValue) > deadZone) {
						valueOfInterest = true;
					}
				}
			}
			if (valueOfInterest) {
				input.setLastValue(value);
				if (isReuseEventObjects()) {
					ReusableInputEvent event = getReusableEventPool().drawFromPool();
					if (event != null) {
						event.reuse(input, value);
					} else {
						event = new ReusableInputEvent(input, value);
						getReusableEventPool().addToPool(event);
					}
					events.add(event);
				} else {
					events.add(new InputEvent(input, value));
				}
			}
		}
	}

	@Override
	public synchronized List<InputDevice> getInputDevices() {
		Controller[] controllers = getControllers();
		List<InputDevice> devices = new Vector<InputDevice>(controllers.length);
		for (int i = 0; i < controllers.length; i++) {
			Controller controller = controllers[i];
			devices.add(mapToDevice(controller));
		}
		return devices;
	}

	@Override
	public synchronized void scanInputDevices() {
		log("Scanning input devices");
		if (isRenewControllerEnvironmentForScan()) {
			forceRenewControllerEnvironment();
		}
		controllers = null;
		int count = getControllers().length; // refresh controllers list
		log("Scanning found " + count + " device" + (count > 1 ? "s" : ""));
	}

	private void forceRenewControllerEnvironment() {
		log("Renewing controller environment");
		controllerEnvironment = null;
		controllers = null;
	}

	private ControllerEnvironment getControllerEnvironment() {
		if (controllerEnvironment == null) {
			controllerEnvironment = new PublicDefaultControllerEnvironment();
			controllerEnvironmentCreationTime = System.currentTimeMillis();
			controllerEnvironmentStartupMaskTime = DEFAULT_CE_STARTUP_MASK_TIME;
		}
		return controllerEnvironment;
	}

	private Controller[] getControllers() {
		if (controllers == null) {
			controllers = getControllerEnvironment().getControllers();
		}
		return controllers;
	}

	private void log(String message) {
		System.out.println(this.getClass().getSimpleName() + " - " + message);
	}

	private boolean isMaskEvents() {
		return System.currentTimeMillis() < getControllerEnvironmentCreationTime()
				+ getControllerEnvironmentStartupMaskTime();
	}

	private long getControllerEnvironmentCreationTime() {
		return controllerEnvironmentCreationTime;
	}

	private long getControllerEnvironmentStartupMaskTime() {
		return controllerEnvironmentStartupMaskTime;
	}

	private JInput mapToInput(Controller controller, Component component) {
		JInput input = inputMap.get(component);
		if (input == null) {
			input = new JInput(mapToDevice(controller), component);
			inputMap.put(component, input);
		}
		return input;
	}

	private JInputDevice mapToDevice(Controller controller) {
		JInputDevice device = deviceMap.get(controller);
		if (device == null) {
			device = new JInputDevice(controller);
			deviceMap.put(controller, device);
		}
		return device;
	}

	public float getOvershootValueChange() {
		return overshootValueChange;
	}

	public void setOvershootValueChange(float valueChange) {
		this.overshootValueChange = valueChange;
	}

	public float getMinimumAnalogDeadZone() {
		return minimumAnalogDeadZone;
	}

	public void setMinimumAnalogDeadZone(float value) {
		this.minimumAnalogDeadZone = value;
	}

	public boolean isExposeEventsInDeadzone() {
		return exposeEventsInDeadzone;
	}

	public void setExposeEventsInDeadzone(boolean expose) {
		this.exposeEventsInDeadzone = expose;
	}

	public boolean isRenewControllerEnvironmentForScan() {
		return renewControllerEnvironmentForScan;
	}

	public void setRenewControllerEnvironmentForScan(boolean renew) {
		this.renewControllerEnvironmentForScan = renew;
	}

	public boolean isReuseEventObjects() {
		return reuseEventObjects;
	}

	public void setReuseEventObjects(boolean reuse) {
		this.reuseEventObjects = reuse;
	}

	private List<InputEvent> getEvents() {
		return events;
	}

	private ReusableEventPool getReusableEventPool() {
		return reusableEventPool;
	}

	private static class ReusableInputEvent extends InputEvent {

		public ReusableInputEvent(Input input, float value) {
			super(input, value);
		}

		public void reuse(Input input, float value) {
			setInput(input);
			setValue(value);
			setCreationTime(System.currentTimeMillis());
		}

	}

	/**
	 * Not thread-safe
	 */
	private static class ReusableEventPool {

		private List<ReusableInputEvent> events; // reusable events

		private int index; // next element index to draw from pool

		private int maximumIndexInCurrentCycle = -1; // last element index to draw from pool in current cycle

		public ReusableEventPool() {
			this.events = new Vector<ReusableInputEvent>();
		}

		public void recycle() {
			setIndex(0);
			setMaximumIndexInCurrentCycle(getPoolSize() - 1);
		}

		public ReusableInputEvent drawFromPool() {
			ReusableInputEvent event = null;
			int i = getIndex();
			if (i <= getMaximumIndexInCurrentCycle()) {
				event = getEvents().get(i);
				setIndex(i + 1);
			}
			return event;
		}

		public void addToPool(ReusableInputEvent event) {
			getEvents().add(event);
		}

		public int getPoolSize() {
			return getEvents().size();
		}

		private List<ReusableInputEvent> getEvents() {
			return events;
		}

		private int getIndex() {
			return index;
		}

		private void setIndex(int index) {
			this.index = index;
		}

		private int getMaximumIndexInCurrentCycle() {
			return maximumIndexInCurrentCycle;
		}

		private void setMaximumIndexInCurrentCycle(int maxIndex) {
			this.maximumIndexInCurrentCycle = maxIndex;
		}

	}

}