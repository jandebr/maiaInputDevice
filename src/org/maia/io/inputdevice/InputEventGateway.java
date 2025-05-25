package org.maia.io.inputdevice;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import org.maia.io.inputdevice.impl.jinput.JInputEventSource;
import org.maia.util.GenericListener;
import org.maia.util.GenericListenerList;

public class InputEventGateway implements InputEventListener {

	private static InputEventGateway instance; // there can be at most 1 gateway

	private InputEventDispatcher dispatcher; // there can be at most 1 running dispatcher at any given time

	private InputEventSource eventSource; // there can be at most 1 event source at any given time

	private GenericListenerList<InputEventListener> globalListeners;

	private GenericListenerList<InputDeviceListener> deviceListeners;

	/**
	 * When true, an external client needs to drive the polling
	 * 
	 * @see #pollExternally()
	 */
	private boolean externalPollMode;

	/**
	 * When true, periodically scans for new devices
	 * <p>
	 * Only effective when <em>externalPollMode</em> is <code>false</code>
	 * </p>
	 * 
	 * @see #isExternalPollMode()
	 */
	private boolean scanMode;

	private InputEventGateway(InputEventSource initialEventSource) {
		this.globalListeners = new GenericListenerList<InputEventListener>();
		this.deviceListeners = new GenericListenerList<InputDeviceListener>();
		switchEventSource(initialEventSource);
	}

	public InputFilter createExplicitUserGestureInputFilter() {
		InputFilter filter = null;
		InputEventSource eventSource = getEventSource();
		if (eventSource != null) {
			filter = eventSource.createExplicitUserGestureInputFilter();
		}
		return filter;
	}

	public void switchEventSource(InputEventSource eventSource) {
		setEventSource(eventSource);
		setupDispatcher();
	}

	public void registerGlobalListener(InputEventListener listener) {
		if (getGlobalListeners().addListener(listener)) {
			setupDispatcher();
		}
	}

	public void unregisterGlobalListener(InputEventListener listener) {
		if (getGlobalListeners().removeListener(listener)) {
			setupDispatcher();
		}
	}

	public void registerDeviceListener(InputDevice device, InputEventListener listener) {
		InputDeviceListener idl = new InputDeviceListener(device, listener);
		if (getDeviceListeners().addListener(idl)) {
			setupDispatcher();
		}
	}

	public void unregisterDeviceListener(InputDevice device, InputEventListener listener) {
		InputDeviceListener idl = new InputDeviceListener(device, listener);
		if (getDeviceListeners().removeListener(idl)) {
			setupDispatcher();
		}
	}

	public void unregisterAllListeners() {
		if (hasListenersRegistered()) {
			getGlobalListeners().removeAllListeners();
			getDeviceListeners().removeAllListeners();
			setupDispatcher();
		}
	}

	private synchronized void setupDispatcher() {
		InputEventSource eventSource = getEventSource();
		InputEventDispatcher dispatcher = getDispatcher();
		if (dispatcher != null) {
			if (isDispatcherNeeded()) {
				dispatcher.setEventSource(eventSource);
			} else {
				dispatcher.removeListener(this);
				dispatcher.stopDispatching();
				setDispatcher(null);
			}
		} else if (isDispatcherNeeded()) {
			dispatcher = new InputEventDispatcher(eventSource);
			setDispatcher(dispatcher);
			dispatcher.setScanMode(isScanMode());
			dispatcher.addListener(this);
			dispatcher.startDispatching();
		}
	}

	@Override
	public void receiveInputEvent(InputEvent event) {
		for (InputEventListener listener : getGlobalListeners()) {
			listener.receiveInputEvent(event);
		}
		for (InputDeviceListener deviceListener : getDeviceListeners()) {
			if (deviceListener.getDevice().equals(event.getDevice())) {
				InputEventListener listener = deviceListener.getListener();
				if (!getGlobalListeners().containsListener(listener)) {
					listener.receiveInputEvent(event);
				}
			}
		}
	}

	public void pollExternally() {
		InputEventSource eventSource = getEventSource();
		if (eventSource != null && isExternalPollMode()) {
			Collection<InputEvent> events = new Vector<InputEvent>();
			synchronized (eventSource) {
				events.addAll(eventSource.pollEvents());
			}
			for (InputEvent event : events) {
				receiveInputEvent(event);
			}
		}
	}

	public void scanInputDevices() {
		InputEventSource eventSource = getEventSource();
		if (eventSource != null) {
			eventSource.scanInputDevices();
		}
	}

	public List<InputDevice> getInputDevices() {
		InputEventSource eventSource = getEventSource();
		if (eventSource != null) {
			return eventSource.getInputDevices();
		} else {
			return Collections.emptyList();
		}
	}

	public InputDevice getInputDeviceWithIdentifier(String identifier) {
		InputEventSource eventSource = getEventSource();
		if (eventSource != null) {
			return eventSource.getInputDeviceWithIdentifier(identifier);
		} else {
			return null;
		}
	}

	public Input getInputWithIdentifier(String deviceIdentifier, String inputIdentifier) {
		InputDevice device = getInputDeviceWithIdentifier(deviceIdentifier);
		if (device != null) {
			return device.getInputWithIdentifier(inputIdentifier);
		} else {
			return null;
		}
	}

	public synchronized void runOutsideDispatcherThread(Runnable task) {
		if (isRunningOnDispatcherThread()) {
			new Thread(task).start();
		} else {
			task.run();
		}
	}

	public synchronized boolean isRunningOnDispatcherThread() {
		return Thread.currentThread().equals(getDispatcher());
	}

	private boolean isDispatcherNeeded() {
		return getEventSource() != null && hasListenersRegistered() && !isExternalPollMode();
	}

	private boolean hasListenersRegistered() {
		return !getGlobalListeners().isEmpty() || !getDeviceListeners().isEmpty();
	}

	private InputEventDispatcher getDispatcher() {
		return dispatcher;
	}

	private void setDispatcher(InputEventDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	public InputEventSource getEventSource() {
		return eventSource;
	}

	private void setEventSource(InputEventSource eventSource) {
		this.eventSource = eventSource;
	}

	private GenericListenerList<InputEventListener> getGlobalListeners() {
		return globalListeners;
	}

	private GenericListenerList<InputDeviceListener> getDeviceListeners() {
		return deviceListeners;
	}

	public boolean isExternalPollMode() {
		return externalPollMode;
	}

	public synchronized void setExternalPollMode(boolean pollExternally) {
		if (pollExternally != this.externalPollMode) {
			this.externalPollMode = pollExternally;
			setupDispatcher();
		}
	}

	public boolean isScanMode() {
		return scanMode;
	}

	public synchronized void setScanMode(boolean scanMode) {
		this.scanMode = scanMode;
		if (getDispatcher() != null) {
			getDispatcher().setScanMode(scanMode);
		}
	}

	public static InputEventGateway getInstance() {
		if (instance == null) {
			setInstance(new InputEventGateway(getDefaultInputEventSource()));
		}
		return instance;
	}

	private static synchronized void setInstance(InputEventGateway gateway) {
		if (instance == null) {
			instance = gateway;
		}
	}

	private static InputEventSource getDefaultInputEventSource() {
		return new JInputEventSource();
	}

	private static class InputDeviceListener implements GenericListener {

		private InputDevice device;

		private InputEventListener listener;

		public InputDeviceListener(InputDevice device, InputEventListener listener) {
			this.device = device;
			this.listener = listener;
		}

		@Override
		public int hashCode() {
			return Objects.hash(getDevice(), getListener());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InputDeviceListener other = (InputDeviceListener) obj;
			return Objects.equals(getDevice(), other.getDevice()) && Objects.equals(getListener(), other.getListener());
		}

		public InputDevice getDevice() {
			return device;
		}

		public InputEventListener getListener() {
			return listener;
		}

	}

}