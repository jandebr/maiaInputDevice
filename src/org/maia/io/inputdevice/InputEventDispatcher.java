package org.maia.io.inputdevice;

import java.util.List;
import java.util.Vector;

import org.maia.util.GenericListenerList;
import org.maia.util.SystemUtils;

public class InputEventDispatcher extends Thread {

	private InputEventSource eventSource;

	private GenericListenerList<InputEventListener> listeners;

	private boolean scanMode;

	private long scanIntervalMillis = 3000L; // every 3 seconds

	private boolean stopDispatching;

	private boolean stopped;

	private static final int POLLS_PER_SECOND_LIMIT = 250;

	InputEventDispatcher(InputEventSource eventSource) {
		super("InputEventDispatcher");
		setPriority(Thread.MAX_PRIORITY);
		setDaemon(true);
		setStopped(true);
		this.eventSource = eventSource;
		this.listeners = new GenericListenerList<InputEventListener>();
	}

	public void addListener(InputEventListener listener) {
		getListeners().addListener(listener);
	}

	public void removeListener(InputEventListener listener) {
		getListeners().removeListener(listener);
	}

	public void removeAllListeners() {
		getListeners().removeAllListeners();
	}

	public void startDispatching() {
		start();
	}

	public void stopDispatching() {
		setStopDispatching(true);
	}

	@Override
	public void run() {
		setStopped(false);
		InputEventSource source = getEventSource();
		List<InputEvent> events = new Vector<InputEvent>();
		long lastScanTime = 0;
		long lastEventTime = 0;
		long pollMinInterval = 1000L / POLLS_PER_SECOND_LIMIT;
		while (!isStopDispatching()) {
			long t0 = System.currentTimeMillis();
			if (isScanMode() && t0 >= lastScanTime + getScanIntervalMillis()) {
				source.scanInputDevices();
				lastScanTime = t0;
			}
			synchronized (source) {
				events.clear();
				List<InputEvent> polledEvents = source.pollEvents();
				for (int i = 0; i < polledEvents.size(); i++) {
					events.add(polledEvents.get(i));
				}
			}
			for (InputEvent event : events) {
				if (event.getCreationTime() >= lastEventTime) {
					dispatchEvent(event);
					lastEventTime = event.getCreationTime();
				}
			}
			SystemUtils.sleep(pollMinInterval - (System.currentTimeMillis() - t0));
		}
		setStopped(true);
	}

	private void dispatchEvent(InputEvent event) {
		for (InputEventListener listener : getListeners()) {
			listener.receiveInputEvent(event);
		}
	}

	public InputEventSource getEventSource() {
		return eventSource;
	}

	void setEventSource(InputEventSource eventSource) {
		this.eventSource = eventSource;
	}

	private GenericListenerList<InputEventListener> getListeners() {
		return listeners;
	}

	public boolean isScanMode() {
		return scanMode;
	}

	public void setScanMode(boolean scanMode) {
		this.scanMode = scanMode;
	}

	public long getScanIntervalMillis() {
		return scanIntervalMillis;
	}

	public void setScanIntervalMillis(long scanIntervalMillis) {
		this.scanIntervalMillis = scanIntervalMillis;
	}

	private boolean isStopDispatching() {
		return stopDispatching;
	}

	private void setStopDispatching(boolean stop) {
		this.stopDispatching = stop;
	}

	public boolean isStopped() {
		return stopped;
	}

	private void setStopped(boolean stopped) {
		this.stopped = stopped;
	}

}