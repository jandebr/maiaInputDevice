package org.maia.io.inputdevice.controller;

import java.util.List;
import java.util.Vector;

public class GatedInputCommandRepeater {

	private List<ScheduledEvent> scheduledEvents;

	private ScheduledEventDispatcher scheduledEventDispatcher;

	private static GatedInputCommandRepeater instance;

	private GatedInputCommandRepeater() {
		this.scheduledEvents = new Vector<ScheduledEvent>();
	}

	public synchronized void startAutorepeat(GatedInputController controller, InputCommand command) {
		if (!hasScheduledEventMatching(controller, command)) {
			long targetTimeMillis = System.currentTimeMillis() + controller.getAutoRepeatInitialDelayMillis();
			getScheduledEvents().add(new ScheduledEvent(controller, command, targetTimeMillis));
			updateDispatcher();
		}
	}

	public synchronized void stopAutorepeat(GatedInputController controller, InputCommand command) {
		ScheduledEvent event = getScheduledEventMatching(controller, command);
		if (event != null) {
			getScheduledEvents().remove(event);
			updateDispatcher();
		}
	}

	private void updateDispatcher() {
		ScheduledEventDispatcher dispatcher = getScheduledEventDispatcher();
		if (dispatcher == null) {
			dispatcher = new ScheduledEventDispatcher();
			setScheduledEventDispatcher(dispatcher);
			dispatcher.start();
		} else {
			dispatcher.interrupt();
		}
	}

	private synchronized boolean hasScheduledEventMatching(GatedInputController controller, InputCommand command) {
		for (ScheduledEvent event : getScheduledEvents()) {
			if (event.matches(controller, command))
				return true;
		}
		return false;
	}

	private synchronized ScheduledEvent getScheduledEventMatching(GatedInputController controller,
			InputCommand command) {
		for (ScheduledEvent event : getScheduledEvents()) {
			if (event.matches(controller, command))
				return event;
		}
		return null;
	}

	private List<ScheduledEvent> getScheduledEvents() {
		return scheduledEvents;
	}

	private ScheduledEventDispatcher getScheduledEventDispatcher() {
		return scheduledEventDispatcher;
	}

	private void setScheduledEventDispatcher(ScheduledEventDispatcher dispatcher) {
		this.scheduledEventDispatcher = dispatcher;
	}

	public static GatedInputCommandRepeater getInstance() {
		if (instance == null) {
			setInstance(new GatedInputCommandRepeater());
		}
		return instance;
	}

	private static synchronized void setInstance(GatedInputCommandRepeater repeater) {
		if (instance == null) {
			instance = repeater;
		}
	}

	private static class ScheduledEvent {

		private GatedInputController controller;

		private InputCommand command;

		private long targetTimeMillis;

		public ScheduledEvent(GatedInputController controller, InputCommand command, long targetTimeMillis) {
			this.controller = controller;
			this.command = command;
			this.targetTimeMillis = targetTimeMillis;
		}

		public boolean matches(GatedInputController controller, InputCommand command) {
			return getController().equals(controller) && getCommand().equals(command);
		}

		public boolean isDue() {
			return isDueIn(0L);
		}

		public boolean isDueIn(long timeFromNowMillis) {
			return getTargetTimeMillis() <= System.currentTimeMillis() + timeFromNowMillis;
		}

		public long getTimeUntilDueMillis() {
			return Math.max(getTargetTimeMillis() - System.currentTimeMillis(), 0L);
		}

		public ScheduledEvent createFutureEvent(long timeFromNowMillis) {
			return new ScheduledEvent(getController(), getCommand(), System.currentTimeMillis() + timeFromNowMillis);
		}

		public GatedInputController getController() {
			return controller;
		}

		public InputCommand getCommand() {
			return command;
		}

		public long getTargetTimeMillis() {
			return targetTimeMillis;
		}

	}

	private class ScheduledEventDispatcher extends Thread {

		private List<ScheduledEvent> eventsDue;

		private static final long MAX_SLEEP_TIME_MILLIS = 50L;

		private static final long MAX_ADVANCE_TIME_MILLIS = 3L;

		public ScheduledEventDispatcher() {
			super("ScheduledEventDispatcher");
			setDaemon(true);
			this.eventsDue = new Vector<ScheduledEvent>();
		}

		@Override
		public void run() {
			do {
				dispatchEventsDue();
				long sleepTime = Math.min(getTimeUntilNextScheduledEvent(), MAX_SLEEP_TIME_MILLIS);
				if (sleepTime > 0L) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// re-evaluate for due events
					}
				}
			} while (true);
		}

		private void dispatchEventsDue() {
			List<ScheduledEvent> eventsDue = collectEventsDue();
			for (ScheduledEvent event : eventsDue) {
				GatedInputController controller = event.getController();
				InputCommand command = event.getCommand();
				synchronized (controller) {
					if (hasScheduledEventMatching(controller, command)) { // not cancelled in meantime
						controller.fireInputCommandInAutoRepeat(command);
						synchronized (GatedInputCommandRepeater.this) {
							getScheduledEvents().remove(event);
							getScheduledEvents().add(event.createFutureEvent(controller.getAutoRepeatDelayMillis()));
						}
					}
				}
			}
		}

		private List<ScheduledEvent> collectEventsDue() {
			eventsDue.clear();
			synchronized (GatedInputCommandRepeater.this) {
				for (ScheduledEvent event : getScheduledEvents()) {
					if (event.isDueIn(MAX_ADVANCE_TIME_MILLIS)) {
						eventsDue.add(event);
					}
				}
			}
			return eventsDue;
		}

		private long getTimeUntilNextScheduledEvent() {
			long time = Long.MAX_VALUE;
			synchronized (GatedInputCommandRepeater.this) {
				for (ScheduledEvent event : getScheduledEvents()) {
					time = Math.min(time, event.getTimeUntilDueMillis());
				}
			}
			return time;
		}

	}

}