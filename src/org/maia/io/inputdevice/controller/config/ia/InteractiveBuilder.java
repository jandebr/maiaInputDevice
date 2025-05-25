package org.maia.io.inputdevice.controller.config.ia;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.maia.io.inputdevice.AnalogInputSensitive;
import org.maia.io.inputdevice.InputDevice;
import org.maia.io.inputdevice.InputDeviceFilter;
import org.maia.io.inputdevice.InputEvent;
import org.maia.io.inputdevice.InputEventGateway;
import org.maia.io.inputdevice.InputFilter;
import org.maia.io.inputdevice.InputValueRange;
import org.maia.io.inputdevice.controller.GatedInputController;
import org.maia.io.inputdevice.controller.GatedInputControllerListener;
import org.maia.io.inputdevice.controller.GatedInputSelector;
import org.maia.io.inputdevice.controller.InputCommand;
import org.maia.io.inputdevice.controller.InputCommandGroup;
import org.maia.io.inputdevice.controller.InputControllerException;
import org.maia.io.inputdevice.controller.InputControllerType;
import org.maia.io.inputdevice.controller.InputSelector;
import org.maia.io.inputdevice.controller.config.InputCommandConfiguration;
import org.maia.io.inputdevice.controller.config.InputControllerByConfigurationBuilder;
import org.maia.io.inputdevice.controller.config.InputControllerConfiguration;
import org.maia.io.inputdevice.controller.config.InputControllerConfigurationBuilder;
import org.maia.io.inputdevice.controller.config.InputControllerContextConfiguration;
import org.maia.io.inputdevice.tuner.InputTuner;
import org.maia.io.inputdevice.tuner.InputTunerListener;
import org.maia.util.GenericListenerList;

public class InteractiveBuilder extends InputControllerConfigurationBuilder
		implements InputTunerListener, AnalogInputSensitive {

	private InputControllerType controllerType;

	private String controllerName;

	private String deviceIdentifier;

	private boolean concurrentlyFiringCommandsEnabled; // ability to fire different commands concurrently

	private boolean fastReleasing; // ability to preemptively release at decline of an input's value

	private List<InputCommandGroup> commandGroups; // input assignments are unique within each group

	private List<InputCommand> uniqueCommands;

	private int indexIntoUniqueCommands;

	private Map<InputCommand, GatedInputSelector> commandAssignments;

	private RequiredInputCommands requiredCommands;

	private InputDeviceFilter acceptedDevices;

	private InputFilter acceptedInputs;

	private float analogSensitivity;

	private InputTuner tuner;

	private boolean interactionStopped;

	private GenericListenerList<InteractiveBuilderListener> listeners;

	private InteractiveBuilderControls controls;

	private InteractiveBuilderController builderController;

	private static final String INITIAL_CONTROLLER_NAME = "$name";

	private static final String INITIAL_DEVICE_IDENTIFIER = "$device";

	public InteractiveBuilder(InputCommandGroup commands, InputDeviceFilter acceptedDevices) {
		this(Collections.singletonList(commands), acceptedDevices);
	}

	public InteractiveBuilder(InputCommandGroup commands, InputDeviceFilter acceptedDevices,
			InputFilter acceptedInputs) {
		this(Collections.singletonList(commands), acceptedDevices, acceptedInputs);
	}

	public InteractiveBuilder(List<InputCommandGroup> commandGroups, InputDeviceFilter acceptedDevices) {
		this(commandGroups, acceptedDevices, InputEventGateway.getInstance().createExplicitUserGestureInputFilter());
	}

	public InteractiveBuilder(List<InputCommandGroup> commandGroups, InputDeviceFilter acceptedDevices,
			InputFilter acceptedInputs) {
		this(commandGroups, acceptedDevices, acceptedInputs, InputTuner.DEFAULT_ANALOG_SENSITIVITY);
	}

	public InteractiveBuilder(List<InputCommandGroup> commandGroups, InputDeviceFilter acceptedDevices,
			InputFilter acceptedInputs, float analogSensitivity) {
		this.commandGroups = commandGroups;
		this.commandAssignments = new HashMap<InputCommand, GatedInputSelector>();
		this.requiredCommands = RequiredInputCommands.REQUIRE_ALL;
		this.acceptedDevices = acceptedDevices != null ? acceptedDevices : InputDeviceFilter.ACCEPT_ALL;
		this.acceptedInputs = acceptedInputs != null ? acceptedInputs : InputFilter.ACCEPT_ALL;
		this.listeners = new GenericListenerList<InteractiveBuilderListener>();
		this.builderController = new InteractiveBuilderController();
		this.controllerType = getDefaultInputControllerType();
		this.controllerName = INITIAL_CONTROLLER_NAME;
		this.deviceIdentifier = INITIAL_DEVICE_IDENTIFIER; // automatically set on first command assignment
		this.concurrentlyFiringCommandsEnabled = InputControllerConfiguration.DEFAULT_CONCURRENT_COMMANDS;
		this.fastReleasing = InputControllerConfiguration.DEFAULT_FAST_RELEASING;
		this.interactionStopped = true;
		changeAnalogSensitivity(analogSensitivity);
	}

	public synchronized void reset() {
		clearAllCommandAssignments();
		moveToFirstCommand();
	}

	public synchronized void resetTo(InputControllerConfiguration configuration) {
		reset();
		if (configuration == null)
			return;
		if (configuration.getControllerType() != null)
			withControllerType(configuration.getControllerType());
		if (configuration.getControllerName() != null)
			withControllerName(configuration.getControllerName());
		setConcurrentlyFiringCommandsEnabled(configuration.isConcurrentlyFiringCommandsEnabled());
		setFastReleasing(configuration.isFastReleasing());
		boolean deviceAssignmentFired = false;
		boolean analogSensitivityAdapted = false; // adapt to the first encountered in configuration
		for (InputControllerContextConfiguration contextConfiguration : configuration.getContextConfigurations()) {
			for (InputCommandConfiguration commandConfiguration : contextConfiguration.getCommandConfigurations()) {
				InputCommand inputCommand = commandConfiguration.getInputCommand();
				InputSelector inputSelector = commandConfiguration.getInputSelector();
				if (inputSelector instanceof GatedInputSelector) {
					GatedInputSelector gatedInputSelector = (GatedInputSelector) inputSelector;
					if (assignCommand(inputCommand, gatedInputSelector, true)) {
						if (!deviceAssignmentFired) {
							fireDeviceAssignment();
							deviceAssignmentFired = true;
						}
						if (!analogSensitivityAdapted) {
							InputValueRange inputFiringRange = gatedInputSelector.getInputFiringRange();
							if (inputFiringRange instanceof AnalogInputSensitive) {
								float sensitivity = ((AnalogInputSensitive) inputFiringRange).getAnalogSensitivity();
								changeAnalogSensitivity(sensitivity);
								analogSensitivityAdapted = true;
							}
						}
					}
				}
			}
		}
		fireChangeOfCommandAssignments();
		if (!isEveryCommandAssigned()) {
			rollToNextUnassignedCommand();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Interactive Configuration Builder [\n");
		for (InputCommand command : getUniqueCommands()) {
			sb.append(command.equals(getCurrentCommand()) ? "> " : "  ");
			sb.append(command.getName());
			sb.append(isCommandRequired(command) ? "*" : "");
			if (isCommandAssigned(command)) {
				sb.append(" <= ");
				sb.append(getCommandAssignment(command));
			}
			sb.append("\n");
		}
		sb.append(isAtEndOfCommands() ? "> " : "  ");
		sb.append(canSubmitViaControls() ? "<submit>" : "<end>").append("\n");
		sb.append("]");
		return sb.toString();
	}

	public void addListener(InteractiveBuilderListener listener) {
		getListeners().addListener(listener);
	}

	public void removeListener(InteractiveBuilderListener listener) {
		getListeners().removeListener(listener);
	}

	public void removeAllListeners() {
		getListeners().removeAllListeners();
	}

	public synchronized void startInteraction() {
		if (isInteractionStopped()) {
			setInteractionStopped(false);
			setTuner(new InputTuner(getAcceptedDevices(), getAcceptedInputs(), getAnalogSensitivity()));
			getTuner().addListener(this);
			getTuner().startTuning(true);
			scanInputDevices();
			updateScanMode();
			fireChangeOfCurrentCommand();
		}
	}

	public synchronized void stopInteraction() {
		if (!isInteractionStopped()) {
			setInteractionStopped(true);
			getTuner().removeListener(this);
			getTuner().stopTuning();
			setTuner(null);
			updateBuilderController();
			updateScanMode();
		}
	}

	public void moveToCommand(InputCommand command) {
		int i = getIndexIntoUniqueCommands(command);
		if (i >= 0) {
			moveToCommandIndex(i);
		}
	}

	public void moveToFirstCommand() {
		moveToCommandIndex(0);
	}

	public void moveToLastCommand() {
		moveToCommandIndex(Math.max(getNumberOfUniqueCommands() - 1, 0));
	}

	public void moveToEnd() {
		moveToCommandIndex(getNumberOfUniqueCommands());
	}

	public void moveToNextCommand() {
		moveToCommandIndex(Math.min(getIndexIntoUniqueCommands() + 1, getNumberOfUniqueCommands()));
	}

	public void moveToPreviousCommand() {
		moveToCommandIndex(Math.max(getIndexIntoUniqueCommands() - 1, 0));
	}

	public void rollToNextCommand() {
		if (isAtEndOfCommands()) {
			moveToFirstCommand();
		} else if (getIndexIntoUniqueCommands() == getNumberOfUniqueCommands() - 1) {
			if (isEveryRequiredCommandAssigned()) {
				moveToEnd();
			} else {
				moveToFirstCommand();
			}
		} else {
			moveToNextCommand();
		}
	}

	public void rollToPreviousCommand() {
		if (getIndexIntoUniqueCommands() == 0) {
			if (isEveryRequiredCommandAssigned()) {
				moveToEnd();
			} else {
				moveToLastCommand();
			}
		} else {
			moveToPreviousCommand();
		}
	}

	public void rollToNextUnassignedCommand() {
		if (!isEveryCommandAssigned()) {
			List<InputCommand> commands = getUniqueCommands();
			int n = commands.size();
			int i = getIndexIntoUniqueCommands();
			int j = 0;
			while (j < n && isCommandAssigned(commands.get((i + j) % n)))
				j++;
			moveToCommandIndex((i + j) % n);
		}
	}

	protected void moveToCommandIndex(int indexIntoUniqueCommands) {
		if (getIndexIntoUniqueCommands() != indexIntoUniqueCommands) {
			setIndexIntoUniqueCommands(indexIntoUniqueCommands);
			fireChangeOfCurrentCommand();
		}
	}

	public synchronized void clearCurrentCommandAssignment() {
		InputCommand command = getCurrentCommand();
		if (isCommandAssigned(command)) {
			getCommandAssignments().remove(command);
			fireChangeOfCommandAssignments();
			updateBuilderController();
			if (!hasCommandAssignments()) {
				resetDeviceIdentifier();
			}
			updateScanMode();
		}
	}

	protected synchronized void clearAllCommandAssignments() {
		if (hasCommandAssignments()) {
			getCommandAssignments().clear();
			resetDeviceIdentifier();
			fireChangeOfCommandAssignments();
			updateBuilderController();
			updateScanMode();
		}
	}

	public void submitCommandAssignments() {
		fireSubmitCommandAssignments();
	}

	public boolean canSubmitViaControls() {
		return !isInteractionStopped() && isAtEndOfCommands() && isEveryRequiredCommandAssigned()
				&& !getTuner().isWaitingForSilence();
	}

	@Override
	public InputControllerConfiguration build() {
		InputControllerConfiguration configuration = new InputControllerConfiguration(getControllerType(),
				getControllerName(), getDeviceIdentifier());
		configuration.setConcurrentlyFiringCommandsEnabled(isConcurrentlyFiringCommandsEnabled());
		configuration.setFastReleasing(isFastReleasing());
		for (InputCommandGroup group : getCommandGroups()) {
			configuration.addContextConfiguration(buildContextConfiguration(group));
		}
		return configuration;
	}

	private InputControllerContextConfiguration buildContextConfiguration(InputCommandGroup group) {
		InputControllerContextConfiguration contextConfiguration = new InputControllerContextConfiguration(
				group.getGroupName());
		for (InputCommand command : group.getMembers()) {
			if (isCommandAssigned(command)) {
				GatedInputSelector inputSelector = getCommandAssignment(command);
				contextConfiguration.addCommandConfiguration(new InputCommandConfiguration(command, inputSelector));
			}
		}
		return contextConfiguration;
	}

	@Override
	public void notifyTunerWaitingForSilence(InputTuner tuner) {
		// no action
	}

	@Override
	public synchronized void notifyTunerSilenceReached(InputTuner tuner) {
		// following a command assignment
		updateBuilderController();
	}

	@Override
	public synchronized void notifyTunerEvent(InputTuner tuner, InputEvent event, InputValueRange tuningRange) {
		if (!isAtEndOfCommands()) {
			GatedInputSelector inputSelector = new GatedInputSelector(event.getDevice().getIdentifier(),
					event.getInput().getIdentifier(), event.getInput().getName(), tuningRange);
			if (!getBuilderController().isActiveControl(getCommandAssignedTo(inputSelector))) {
				if (assignCommand(getCurrentCommand(), inputSelector, false)) {
					if (!isEveryCommandAssigned()) {
						rollToNextUnassignedCommand();
					} else if (getIndexIntoUniqueCommands() == getNumberOfUniqueCommands() - 1) {
						moveToEnd();
					} else {
						// stay in place
					}
					tuner.waitForSilence();
				}
			}
		}
	}

	protected synchronized boolean assignCommand(InputCommand command, GatedInputSelector inputSelector,
			boolean suppressListenerNotifications) {
		boolean accepted = acceptCommandAssignment(command, inputSelector);
		if (accepted) {
			if (!hasCommandAssignments()) {
				// Device selected
				setDeviceIdentifier(inputSelector.getDeviceIdentifier());
				if (!suppressListenerNotifications) {
					fireDeviceAssignment();
				}
			}
			getCommandAssignments().put(command, inputSelector);
			if (!suppressListenerNotifications) {
				fireCommandAssignment(command);
				fireChangeOfCommandAssignments();
			}
			updateScanMode();
		}
		return accepted;
	}

	protected boolean acceptCommandAssignment(InputCommand command, GatedInputSelector inputSelector) {
		if (command == null || inputSelector == null)
			return false;
		if (!getUniqueCommands().contains(command))
			return false;
		if (!isSameDevice(inputSelector))
			return false;
		if (isSameCommandAssignment(command, inputSelector))
			return false;
		if (!isUniqueWithinParticipatingGroups(command, inputSelector))
			return false;
		return true;
	}

	private boolean isSameDevice(GatedInputSelector inputSelector) {
		if (!hasCommandAssignments()) {
			return true;
		} else {
			return inputSelector.getDeviceIdentifier().equals(getDeviceIdentifier());
		}
	}

	private boolean isSameCommandAssignment(InputCommand command, GatedInputSelector inputSelector) {
		return isCommandAssigned(command) && getCommandAssignment(command).equals(inputSelector);
	}

	private boolean isUniqueWithinParticipatingGroups(InputCommand command, GatedInputSelector inputSelector) {
		Set<InputCommand> fellows = getFellowGroupMembers(command);
		for (InputCommand fellowCommand : fellows) {
			if (isCommandAssigned(fellowCommand) && getCommandAssignment(fellowCommand).equals(inputSelector))
				return false;
		}
		return true;
	}

	protected void fireChangeOfCurrentCommand() {
		for (InteractiveBuilderListener listener : getListeners()) {
			listener.notifyChangeOfCurrentCommand(this);
		}
	}

	protected void fireDeviceAssignment() {
		for (InteractiveBuilderListener listener : getListeners()) {
			listener.notifyDeviceAssignment(this, getDeviceIdentifier());
		}
	}

	protected void fireCommandAssignment(InputCommand command) {
		for (InteractiveBuilderListener listener : getListeners()) {
			listener.notifyCommandAssignment(this, command);
		}
	}

	protected void fireChangeOfCommandAssignments() {
		for (InteractiveBuilderListener listener : getListeners()) {
			listener.notifyChangeOfCommandAssignments(this);
		}
	}

	protected void fireSubmitCommandAssignments() {
		for (InteractiveBuilderListener listener : getListeners()) {
			listener.notifySubmitCommandAssignments(this);
		}
	}

	@Override
	public synchronized void changeAnalogSensitivity(float sensitivity) {
		if (sensitivity < MINIMUM_SENSITIVITY_INCLUSIVE || sensitivity >= MAXIMUM_SENSITIVITY_EXCLUSIVE)
			throw new IllegalArgumentException("Sensitivity value out of range: " + sensitivity);
		setAnalogSensitivity(sensitivity);
		applyAnalogSensitivityOnTuner();
		applyAnalogSensitivityOnCommandAssignments();
	}

	private void applyAnalogSensitivityOnTuner() {
		InputTuner tuner = getTuner();
		if (tuner != null) {
			tuner.changeAnalogSensitivity(getAnalogSensitivity());
		}
	}

	private void applyAnalogSensitivityOnCommandAssignments() {
		float sensitivity = getAnalogSensitivity();
		for (InputCommand command : getCommandAssignments().keySet()) {
			InputValueRange inputFiringRange = getCommandAssignment(command).getInputFiringRange();
			if (inputFiringRange instanceof AnalogInputSensitive) {
				((AnalogInputSensitive) inputFiringRange).changeAnalogSensitivity(sensitivity);
			}
		}
	}

	public void scanInputDevices() {
		InputEventGateway.getInstance().scanInputDevices();
		updateBuilderController();
	}

	protected void updateScanMode() {
		// if there are any command assignments, stop scan mode and stick to the device
		boolean scanMode = !isInteractionStopped() && !hasCommandAssignments();
		InputEventGateway.getInstance().setScanMode(scanMode);
	}

	protected synchronized void updateBuilderController() {
		getBuilderController().update();
	}

	private void resetDeviceIdentifier() {
		setDeviceIdentifier(INITIAL_DEVICE_IDENTIFIER);
	}

	public Set<InputCommand> getFellowGroupMembers(InputCommand command) {
		Set<InputCommand> fellows = new HashSet<InputCommand>();
		for (InputCommandGroup group : getCommandGroups()) {
			if (group.hasMember(command)) {
				fellows.addAll(group.getMembers());
			}
		}
		fellows.remove(command); // exclude self
		return fellows;
	}

	public InputCommand getCurrentCommand() {
		if (isAtEndOfCommands()) {
			return null;
		} else {
			return getUniqueCommands().get(getIndexIntoUniqueCommands());
		}
	}

	public int getNumberOfUniqueCommands() {
		return getUniqueCommands().size();
	}

	public boolean isAtEndOfCommands() {
		return getIndexIntoUniqueCommands() == getNumberOfUniqueCommands();
	}

	public boolean isEveryCommandAssigned() {
		return getCommandAssignments().size() == getNumberOfUniqueCommands();
	}

	public boolean isEveryRequiredCommandAssigned() {
		for (InputCommand command : getUniqueCommands()) {
			if (isCommandRequired(command) && !isCommandAssigned(command))
				return false;
		}
		return true;
	}

	public boolean isCommandAssigned(InputCommand command) {
		return getCommandAssignments().containsKey(command);
	}

	public boolean isCommandRequired(InputCommand command) {
		return getRequiredCommands().isRequired(command);
	}

	public GatedInputSelector getCommandAssignment(InputCommand command) {
		return getCommandAssignments().get(command);
	}

	private InputCommand getCommandAssignedTo(GatedInputSelector inputSelector) {
		for (InputCommand command : getCommandAssignments().keySet()) {
			if (getCommandAssignment(command).equals(inputSelector))
				return command;
		}
		return null;
	}

	public boolean hasCommandAssignments() {
		return !getCommandAssignments().isEmpty();
	}

	public InteractiveBuilder withControllerType(InputControllerType controllerType) {
		this.controllerType = controllerType;
		return this;
	}

	public InteractiveBuilder withControllerName(String controllerName) {
		this.controllerName = controllerName;
		return this;
	}

	public InteractiveBuilder withControls(InteractiveBuilderControls controls) {
		this.controls = controls;
		return this;
	}

	public InteractiveBuilder withRequiredCommands(RequiredInputCommands requiredCommands) {
		this.requiredCommands = requiredCommands;
		return this;
	}

	public InputControllerType getControllerType() {
		return controllerType;
	}

	public String getControllerName() {
		return controllerName;
	}

	public InputDevice getDevice() {
		InputDevice device = null;
		String deviceIdentifier = getDeviceIdentifier();
		if (deviceIdentifier != null) {
			device = InputEventGateway.getInstance().getInputDeviceWithIdentifier(deviceIdentifier);
		}
		return device;
	}

	public String getDeviceIdentifier() {
		return deviceIdentifier;
	}

	private void setDeviceIdentifier(String deviceIdentifier) {
		this.deviceIdentifier = deviceIdentifier;
	}

	public boolean isConcurrentlyFiringCommandsEnabled() {
		return concurrentlyFiringCommandsEnabled;
	}

	public void setConcurrentlyFiringCommandsEnabled(boolean enabled) {
		this.concurrentlyFiringCommandsEnabled = enabled;
	}

	public boolean isFastReleasing() {
		return fastReleasing;
	}

	public void setFastReleasing(boolean fastReleasing) {
		this.fastReleasing = fastReleasing;
	}

	public List<InputCommandGroup> getCommandGroups() {
		return commandGroups;
	}

	public List<InputCommand> getUniqueCommands() {
		if (uniqueCommands == null) {
			uniqueCommands = new Vector<InputCommand>();
			for (InputCommandGroup group : getCommandGroups()) {
				for (InputCommand command : group.getMembers()) {
					if (!uniqueCommands.contains(command))
						uniqueCommands.add(command);
				}
			}
		}
		return uniqueCommands;
	}

	protected int getIndexIntoUniqueCommands(InputCommand command) {
		return getUniqueCommands().indexOf(command);
	}

	private int getIndexIntoUniqueCommands() {
		return indexIntoUniqueCommands;
	}

	private void setIndexIntoUniqueCommands(int index) {
		this.indexIntoUniqueCommands = index;
	}

	private Map<InputCommand, GatedInputSelector> getCommandAssignments() {
		return commandAssignments;
	}

	public RequiredInputCommands getRequiredCommands() {
		return requiredCommands;
	}

	public InputDeviceFilter getAcceptedDevices() {
		return acceptedDevices;
	}

	public InputFilter getAcceptedInputs() {
		return acceptedInputs;
	}

	@Override
	public float getAnalogSensitivity() {
		return analogSensitivity;
	}

	private void setAnalogSensitivity(float sensitivity) {
		this.analogSensitivity = sensitivity;
	}

	private InputTuner getTuner() {
		return tuner;
	}

	private void setTuner(InputTuner tuner) {
		this.tuner = tuner;
	}

	public boolean isInteractionStopped() {
		return interactionStopped;
	}

	private void setInteractionStopped(boolean stopped) {
		this.interactionStopped = stopped;
	}

	private GenericListenerList<InteractiveBuilderListener> getListeners() {
		return listeners;
	}

	public InteractiveBuilderControls getControls() {
		return controls;
	}

	private InteractiveBuilderController getBuilderController() {
		return builderController;
	}

	private class InteractiveBuilderController implements GatedInputControllerListener {

		private GatedInputController controller;

		private Set<InputCommand> controllerCommands;

		public InteractiveBuilderController() {
		}

		public void update() {
			if (controller != null) {
				controller.removeGatedListener(this);
				controller.dispose();
				controller = null;
			}
			if (!isInteractionStopped()) {
				controller = createController();
				if (controller != null) {
					controllerCommands = controller.getCurrentContextCommands();
					controller.addGatedListener(this);
				}
			}
		}

		@Override
		public void inputCommandFired(GatedInputController controller, InputCommand command, boolean autoRepeat) {
			InteractiveBuilderControls controls = getControls();
			if (controls != null) {
				InteractiveBuilderControlType type = controls.getType(command);
				if (type != null) {
					if (InteractiveBuilderControlType.PREVIOUS.equals(type)) {
						rollToPreviousCommand();
					} else if (InteractiveBuilderControlType.NEXT.equals(type)) {
						rollToNextCommand();
					} else if (InteractiveBuilderControlType.CLEAR.equals(type)) {
						clearCurrentCommandAssignment();
					} else if (InteractiveBuilderControlType.SUBMIT.equals(type) && canSubmitViaControls()) {
						submitCommandAssignments();
					}
				}
			}
		}

		@Override
		public void inputCommandReleased(GatedInputController controller, InputCommand command) {
		}

		private GatedInputController createController() {
			GatedInputController controller = null;
			if (hasCommandAssignments()) {
				InteractiveBuilder builder = InteractiveBuilder.this;
				InputControllerType type = builder.getControllerType();
				String name = builder.getControllerName();
				InputControllerConfiguration configuration = builder.withControllerType(InputControllerType.GATED)
						.withControllerName("CTR-IA").build();
				configuration.setConcurrentlyFiringCommandsEnabled(false); // not suitable when building
				configuration.setFastReleasing(false); // not suitable when building
				builder.withControllerType(type).withControllerName(name); // restore
				try {
					controller = new InputControllerByConfigurationBuilder(configuration).buildGated();
					controller.setConcurrentlyFiringCommandsEnabled(false);
					controller.setAutoRepeatEnabled(false);
				} catch (InputControllerException e) {
					// most likely cause, input device not (no longer) connected
				}
			}
			return controller;
		}

		public boolean isActiveControl(InputCommand command) {
			InteractiveBuilderControls controls = getControls();
			if (controls != null && command != null) {
				Set<InputCommand> ctrCommands = getControllerCommands();
				if (ctrCommands != null && ctrCommands.contains(command)) {
					InteractiveBuilderControlType type = controls.getType(command);
					if (type != null) {
						if (InteractiveBuilderControlType.SUBMIT.equals(type)) {
							return canSubmitViaControls();
						} else {
							return true;
						}
					}
				}
			}
			return false;
		}

		private Set<InputCommand> getControllerCommands() {
			return controllerCommands;
		}

	}

}