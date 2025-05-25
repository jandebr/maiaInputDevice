package org.maia.io.inputdevice.tuner;

import org.maia.io.inputdevice.AnalogInputSensitive;
import org.maia.io.inputdevice.AnalogInputValueRange;
import org.maia.io.inputdevice.DigitalInputValueRange;
import org.maia.io.inputdevice.InputDeviceFilter;
import org.maia.io.inputdevice.InputEvent;
import org.maia.io.inputdevice.InputEventGateway;
import org.maia.io.inputdevice.InputEventListener;
import org.maia.io.inputdevice.InputFilter;
import org.maia.io.inputdevice.InputValueRange;
import org.maia.util.GenericListenerList;

public class InputTuner implements InputEventListener, AnalogInputSensitive {

	private InputDeviceFilter deviceFilter;

	private InputFilter inputFilter;

	private float analogSensitivity;

	private GenericListenerList<InputTunerListener> listeners;

	private SilenceDetector silenceDetector;

	private boolean waitingForSilence;

	private boolean tuningStopped;

	public static float DEFAULT_ANALOG_SENSITIVITY = 0.6f;

	public static long DEFAULT_SILENCE_DURATION_MILLIS = 250L;

	public static long DEFAULT_SILENCE_TUNINGSTART_MILLIS = 400L;

	public InputTuner(InputDeviceFilter deviceFilter) {
		this(deviceFilter, InputEventGateway.getInstance().createExplicitUserGestureInputFilter());
	}

	public InputTuner(InputDeviceFilter deviceFilter, InputFilter inputFilter) {
		this(deviceFilter, inputFilter, DEFAULT_ANALOG_SENSITIVITY);
	}

	public InputTuner(InputDeviceFilter deviceFilter, InputFilter inputFilter, float analogSensitivity) {
		this.deviceFilter = deviceFilter != null ? deviceFilter : InputDeviceFilter.ACCEPT_ALL;
		this.inputFilter = inputFilter != null ? inputFilter : InputFilter.ACCEPT_ALL;
		this.listeners = new GenericListenerList<InputTunerListener>();
		this.silenceDetector = new SilenceDetector(DEFAULT_SILENCE_DURATION_MILLIS);
		changeAnalogSensitivity(analogSensitivity);
	}

	public void addListener(InputTunerListener listener) {
		getListeners().addListener(listener);
	}

	public void removeListener(InputTunerListener listener) {
		getListeners().removeListener(listener);
	}

	public void removeAllListeners() {
		getListeners().removeAllListeners();
	}

	public synchronized void startTuning(boolean waitForSilence) {
		if (isTuningStopped())
			throw new IllegalStateException("Cannot resume tuning once stopped. Should create a new tuner");
		if (waitForSilence) {
			waitForSilence(DEFAULT_SILENCE_TUNINGSTART_MILLIS);
		}
		getSilenceDetector().start();
		InputEventGateway.getInstance().registerGlobalListener(this);
	}

	public synchronized void stopTuning() {
		if (!isTuningStopped()) {
			setTuningStopped(true);
			InputEventGateway.getInstance().unregisterGlobalListener(this);
			getSilenceDetector().interrupt();
		}
	}

	public synchronized void waitForSilence() {
		waitForSilence(DEFAULT_SILENCE_DURATION_MILLIS);
	}

	public synchronized void waitForSilence(long durationMillis) {
		if (!isWaitingForSilence()) {
			getSilenceDetector().setSilenceDurationMillis(durationMillis);
			setWaitingForSilence(true);
			fireWaitingForSilence();
			if (getSilenceDetector().isAlive()) {
				getSilenceDetector().interrupt();
			}
		}
	}

	@Override
	public void receiveInputEvent(InputEvent event) {
		if (accept(event)) {
			synchronized (this) {
				if (isWaitingForSilence()) {
					getSilenceDetector().interrupt();
				} else {
					fireInputEvent(event, getTuningRangeFor(event));
				}
			}
		}
	}

	private boolean accept(InputEvent event) {
		if (event.getInput().isDigital()) {
			if (event.getValue() <= 0f)
				return false;
		} else {
			if (Math.abs(event.getValue()) < 1.0f - getAnalogSensitivity())
				return false;
		}
		return getDeviceFilter().accept(event.getDevice()) && getInputFilter().accept(event.getInput());
	}

	private InputValueRange getTuningRangeFor(InputEvent event) {
		if (event.getInput().isDigital()) {
			return new DigitalInputValueRange(event.getValue());
		} else {
			InputValueRange inputRange = event.getInput().getValueRange();
			float s = 1.0f - getAnalogSensitivity();
			if (event.getValue() > 0) {
				// positive range
				return new AnalogInputValueRange(s, Math.max(inputRange.getMaximumValue(), s));
			} else {
				// negative range
				return new AnalogInputValueRange(Math.min(inputRange.getMinimumValue(), -s), -s);
			}
		}
	}

	private void fireWaitingForSilence() {
		for (InputTunerListener listener : getListeners()) {
			listener.notifyTunerWaitingForSilence(this);
		}
	}

	private void fireSilenceReached() {
		for (InputTunerListener listener : getListeners()) {
			listener.notifyTunerSilenceReached(this);
		}
	}

	private void fireInputEvent(InputEvent event, InputValueRange tuningRange) {
		for (InputTunerListener listener : getListeners()) {
			listener.notifyTunerEvent(this, event, tuningRange);
		}
	}

	@Override
	protected void finalize() {
		stopTuning();
	}

	@Override
	public void changeAnalogSensitivity(float sensitivity) {
		if (sensitivity < MINIMUM_SENSITIVITY_INCLUSIVE || sensitivity >= MAXIMUM_SENSITIVITY_EXCLUSIVE)
			throw new IllegalArgumentException("Sensitivity value out of range: " + sensitivity);
		setAnalogSensitivity(sensitivity);
	}

	public InputDeviceFilter getDeviceFilter() {
		return deviceFilter;
	}

	public InputFilter getInputFilter() {
		return inputFilter;
	}

	@Override
	public float getAnalogSensitivity() {
		return analogSensitivity;
	}

	private void setAnalogSensitivity(float sensitivity) {
		this.analogSensitivity = sensitivity;
	}

	public long getSilenceDurationMillis() {
		return getSilenceDetector().getSilenceDurationMillis();
	}

	private GenericListenerList<InputTunerListener> getListeners() {
		return listeners;
	}

	private SilenceDetector getSilenceDetector() {
		return silenceDetector;
	}

	public boolean isWaitingForSilence() {
		return waitingForSilence;
	}

	private void setWaitingForSilence(boolean waiting) {
		this.waitingForSilence = waiting;
	}

	private boolean isTuningStopped() {
		return tuningStopped;
	}

	private void setTuningStopped(boolean stopped) {
		this.tuningStopped = stopped;
	}

	private class SilenceDetector extends Thread {

		private long silenceDurationMillis;

		public SilenceDetector(long silenceDurationMillis) {
			super("SilenceDetector");
			setSilenceDurationMillis(silenceDurationMillis);
			setDaemon(true);
		}

		@Override
		public void run() {
			while (!isTuningStopped()) {
				try {
					boolean waitingForSilence = isWaitingForSilence();
					Thread.sleep(getSilenceDurationMillis());
					if (waitingForSilence) {
						fireSilenceReached();
						setWaitingForSilence(false);
					}
				} catch (InterruptedException e) {
					// renew waiting cycle (or tuning stopped)
				}
			}
		}

		public long getSilenceDurationMillis() {
			return silenceDurationMillis;
		}

		public void setSilenceDurationMillis(long millis) {
			this.silenceDurationMillis = millis;
		}

	}

}