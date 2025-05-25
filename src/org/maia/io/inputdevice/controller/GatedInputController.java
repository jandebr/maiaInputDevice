package org.maia.io.inputdevice.controller;

import org.maia.io.inputdevice.AnalogInputValueRange;
import org.maia.io.inputdevice.DigitalInputValueRange;
import org.maia.io.inputdevice.InputDevice;
import org.maia.io.inputdevice.InputValueRange;
import org.maia.util.GenericListenerList;

public class GatedInputController extends InputController {

	public static final float DEFAULT_INPUT_FIRING_VALUE = 1.0f;

	private GenericListenerList<GatedInputControllerListener> gatedListeners;

	private GatedInputCommandRepeater autoRepeater;

	private Object fireReleaseMutex = new Object();

	private boolean autoRepeatEnabled; // by default no auto-repeat

	private long autoRepeatInitialDelayMillis = 500L;

	private long autoRepeatDelayMillis = 30L;

	private boolean concurrentlyFiringCommandsEnabled = true; // ability to fire different commands concurrently

	private InputCommand currentFiringCommand;

	private boolean fastReleasing = true; // ability to preemptively release at decline of an input's value

	public GatedInputController(String name, String deviceIdentifier) throws InputControllerException {
		super(name, deviceIdentifier);
	}

	public GatedInputController(String name, String deviceIdentifier, InputControllerContext initialContext)
			throws InputControllerException {
		super(name, deviceIdentifier, initialContext);
	}

	public GatedInputController(String name, InputDevice device) {
		super(name, device);
	}

	public GatedInputController(String name, InputDevice device, InputControllerContext initialContext) {
		super(name, device, initialContext);
	}

	@Override
	protected void init() {
		super.init();
		setType(InputControllerType.GATED);
		this.gatedListeners = new GenericListenerList<GatedInputControllerListener>();
		this.autoRepeater = GatedInputCommandRepeater.getInstance();
	}

	public void addGatedListener(GatedInputControllerListener listener) {
		getGatedListeners().addListener(listener);
	}

	public void removeGatedListener(GatedInputControllerListener listener) {
		getGatedListeners().removeListener(listener);
	}

	public synchronized void setupGatedCommand(InputCommand command, String inputIdentifier)
			throws InputControllerException {
		setupGatedCommand(command, inputIdentifier, DEFAULT_INPUT_FIRING_VALUE);
	}

	public synchronized void setupGatedCommand(InputCommand command, String inputIdentifier, float inputFiringValue)
			throws InputControllerException {
		setupGatedCommand(command, inputIdentifier, new DigitalInputValueRange(inputFiringValue));
	}

	public synchronized void setupGatedCommand(InputCommand command, String inputIdentifier, float inputFiringMinValue,
			float inputFiringMaxValue) throws InputControllerException {
		setupGatedCommand(command, inputIdentifier,
				new AnalogInputValueRange(inputFiringMinValue, inputFiringMaxValue));
	}

	public synchronized void setupGatedCommand(InputCommand command, String inputIdentifier,
			InputValueRange inputFiringRange) throws InputControllerException {
		setupCommand(new GatedInputCommandProducer(command, inputFiringRange), inputIdentifier);
	}

	public synchronized void setupGatedCommand(InputCommand command, GatedInputSelector inputSelector)
			throws InputControllerException {
		setupCommand(new GatedInputCommandProducer(command, inputSelector.getInputFiringRange()),
				inputSelector.getInputIdentifier());
	}

	protected void fireInputCommand(InputCommand command) {
		if (isActive()) {
			synchronized (fireReleaseMutex) {
				if (isConcurrentlyFiringCommandsEnabled() || !isFiring() || isFiring(command)) {
					setCurrentFiringCommand(command);
					for (GatedInputControllerListener listener : getGatedListeners()) {
						listener.inputCommandFired(this, command, false);
					}
					if (isAutoRepeatEnabled()) {
						getAutoRepeater().startAutorepeat(this, command);
					}
				}
			}
		}
	}

	protected void fireInputCommandInAutoRepeat(InputCommand command) {
		if (isActive()) {
			synchronized (fireReleaseMutex) {
				if (isConcurrentlyFiringCommandsEnabled() || isFiring(command)) {
					setCurrentFiringCommand(command);
					for (GatedInputControllerListener listener : getGatedListeners()) {
						listener.inputCommandFired(this, command, true);
					}
				}
			}
		}
	}

	protected void releaseInputCommand(InputCommand command) {
		// release also when inactive to avoid stuck firing state
		synchronized (fireReleaseMutex) {
			if (isConcurrentlyFiringCommandsEnabled() || isFiring(command)) {
				setCurrentFiringCommand(null);
				getAutoRepeater().stopAutorepeat(this, command);
				for (GatedInputControllerListener listener : getGatedListeners()) {
					listener.inputCommandReleased(this, command);
				}
			}
		}
	}

	protected boolean isFiring() {
		return getCurrentFiringCommand() != null;
	}

	protected boolean isFiring(InputCommand command) {
		return isFiring() && command.equals(getCurrentFiringCommand());
	}

	protected GenericListenerList<GatedInputControllerListener> getGatedListeners() {
		return gatedListeners;
	}

	protected GatedInputCommandRepeater getAutoRepeater() {
		return autoRepeater;
	}

	public boolean isAutoRepeatEnabled() {
		return autoRepeatEnabled;
	}

	public void setAutoRepeatEnabled(boolean enabled) {
		this.autoRepeatEnabled = enabled;
	}

	public long getAutoRepeatInitialDelayMillis() {
		return autoRepeatInitialDelayMillis;
	}

	public void setAutoRepeatInitialDelayMillis(long initialDelayMillis) {
		this.autoRepeatInitialDelayMillis = initialDelayMillis;
	}

	public long getAutoRepeatDelayMillis() {
		return autoRepeatDelayMillis;
	}

	public void setAutoRepeatDelayMillis(long delayMillis) {
		this.autoRepeatDelayMillis = delayMillis;
	}

	public boolean isConcurrentlyFiringCommandsEnabled() {
		return concurrentlyFiringCommandsEnabled;
	}

	public void setConcurrentlyFiringCommandsEnabled(boolean enabled) {
		this.concurrentlyFiringCommandsEnabled = enabled;
	}

	private InputCommand getCurrentFiringCommand() {
		return currentFiringCommand;
	}

	private void setCurrentFiringCommand(InputCommand command) {
		this.currentFiringCommand = command;
	}

	public boolean isFastReleasing() {
		return fastReleasing;
	}

	public void setFastReleasing(boolean fastReleasing) {
		this.fastReleasing = fastReleasing;
	}

}