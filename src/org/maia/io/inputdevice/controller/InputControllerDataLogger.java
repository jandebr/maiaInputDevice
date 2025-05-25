package org.maia.io.inputdevice.controller;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.maia.io.inputdevice.DigitalInputValueRange;
import org.maia.io.inputdevice.Input;
import org.maia.io.inputdevice.InputDevice;
import org.maia.io.inputdevice.InputEvent;
import org.maia.io.inputdevice.InputValueRange;
import org.maia.util.StringUtils;

public class InputControllerDataLogger {

	private List<InputEvent> loggedEvents;

	private boolean paused;

	private static InputControllerDataLogger instance;

	private static final SystemInputDevice systemInputDevice = new SystemInputDevice();

	private InputControllerDataLogger() {
		this.loggedEvents = new Vector<InputEvent>(1000);
	}

	public void pause() {
		setPaused(true);
	}

	public void resume() {
		setPaused(false);
	}

	public void logSystemEvent(String eventIdentifier) {
		logInputEvent(new SystemEvent(eventIdentifier));
	}

	public void logInputEvent(InputEvent event) {
		if (!isPaused()) {
			getLoggedEvents().add(event);
		}
	}

	public void print(PrintStream out) {
		List<InputEvent> events = getChronologicalEvents();
		if (!events.isEmpty()) {
			NumberFormat nf = NumberFormat.getNumberInstance();
			Map<Input, Long> previousInputTime = new HashMap<>();
			long t0 = events.get(0).getCreationTime();
			for (InputEvent event : events) {
				Input input = event.getInput();
				// Relative time
				long t = event.getCreationTime();
				long tr = t - t0;
				long trSec = Math.floorDiv(tr, 1000);
				long trMs = tr - trSec * 1000;
				out.print(StringUtils.leftPad(String.valueOf(trSec), 4, ' '));
				out.print('.');
				out.print(StringUtils.leftPad(String.valueOf(trMs), 3, '0'));
				// Delta time
				long td = 0;
				Long tp = previousInputTime.get(input);
				if (tp != null) {
					td = t - tp.longValue();
				}
				previousInputTime.put(input, t);
				out.print(" (+");
				out.print(StringUtils.leftPad(String.valueOf(td), 4, ' '));
				out.print(')');
				// Input
				out.print(' ');
				out.print(StringUtils.rightPad(input.getIdentifier(), 16, ' '));
				// Value
				if (input.isAnalog()) {
					float value = event.getValue();
					out.print(' ');
					out.print(value < 0f ? '-' : ' ');
					out.print(nf.format(Math.abs(value)));
				}
				out.println();
			}
		}
	}

	private List<InputEvent> getChronologicalEvents() {
		List<InputEvent> events = new Vector<InputEvent>(getLoggedEvents());
		Collections.sort(events, new InputEventChronologicalComparator());
		return events;
	}

	public List<InputEvent> getLoggedEvents() {
		return loggedEvents;
	}

	public boolean isPaused() {
		return paused;
	}

	private void setPaused(boolean paused) {
		this.paused = paused;
	}

	public static InputControllerDataLogger getInstance() {
		if (instance == null) {
			setInstance(new InputControllerDataLogger());
		}
		return instance;
	}

	private static synchronized void setInstance(InputControllerDataLogger logger) {
		if (instance == null) {
			instance = logger;
		}
	}

	private static class SystemEvent extends InputEvent {

		public SystemEvent(String eventIdentifier) {
			super(systemInputDevice.getOrCreateInput(eventIdentifier), 1f);
		}

	}

	private static class SystemInputDevice extends InputDevice {

		private Map<String, SystemInput> inputMap;

		public SystemInputDevice() {
			super("SYSTEM", "SYSTEM");
			this.inputMap = new HashMap<String, SystemInput>();
		}

		public synchronized SystemInput getOrCreateInput(String inputIdentifier) {
			SystemInput input = getInputMap().get(inputIdentifier);
			if (input == null) {
				input = new SystemInput(this, inputIdentifier);
				getInputMap().put(inputIdentifier, input);
			}
			return input;
		}

		@Override
		public String getTypeString() {
			return "SYSTEM";
		}

		@Override
		public boolean isTypeMouse() {
			return false;
		}

		@Override
		public boolean isTypeKeyboard() {
			return false;
		}

		@Override
		public boolean isTypeStick() {
			return false;
		}

		@Override
		public boolean isTypeGamepad() {
			return false;
		}

		@Override
		public boolean isTypeUnknown() {
			return false;
		}

		@Override
		protected List<Input> enumerateInputs() {
			return new Vector<Input>(getInputMap().values());
		}

		private Map<String, SystemInput> getInputMap() {
			return inputMap;
		}

	}

	private static class SystemInput extends Input {

		public SystemInput(SystemInputDevice device, String identifier) {
			super(device, identifier, identifier);
		}

		@Override
		public boolean isAnalog() {
			return false;
		}

		@Override
		public boolean isRelative() {
			return false;
		}

		@Override
		public InputValueRange getValueRange() {
			return new DigitalInputValueRange(1f);
		}

	}

	private static class InputEventChronologicalComparator implements Comparator<InputEvent> {

		public InputEventChronologicalComparator() {
		}

		@Override
		public int compare(InputEvent e1, InputEvent e2) {
			long t1 = e1.getCreationTime();
			long t2 = e2.getCreationTime();
			if (t1 < t2) {
				return -1;
			} else if (t1 > t2) {
				return 1;
			} else {
				return 0;
			}
		}

	}

}