package org.maia.io.inputdevice.controller;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import org.maia.io.inputdevice.Input;
import org.maia.io.inputdevice.InputEvent;

public class InputControllerContext implements InputEventProcessor {

	private String identifier;

	private Map<Input, List<InputEventProcessor>> inputProcessorMap;

	public InputControllerContext(String identifier) {
		this.identifier = identifier;
		this.inputProcessorMap = new HashMap<Input, List<InputEventProcessor>>();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getIdentifier());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof InputControllerContext))
			return false;
		InputControllerContext other = (InputControllerContext) obj;
		return Objects.equals(getIdentifier(), other.getIdentifier());
	}

	public void registerInputProcessor(Input input, InputEventProcessor processor) {
		List<InputEventProcessor> inputProcessors = getInputProcessorMap().get(input);
		if (inputProcessors == null) {
			inputProcessors = new Vector<InputEventProcessor>(4);
			getInputProcessorMap().put(input, inputProcessors);
		}
		inputProcessors.add(processor);
	}

	public void unregisterInputProcessor(Input input, InputEventProcessor processor) {
		List<InputEventProcessor> inputProcessors = getInputProcessorMap().get(input);
		if (inputProcessors != null && inputProcessors.remove(processor)) {
			if (inputProcessors.isEmpty()) {
				unregisterInputProcessors(input);
			}
		}
	}

	public void unregisterInputProcessors(Input input) {
		getInputProcessorMap().remove(input);
	}

	public void unregisterAllInputProcessors() {
		getInputProcessorMap().clear();
	}

	@Override
	public void init(InputController controller) {
		for (InputEventProcessor processor : getInputProcessors()) {
			processor.init(controller);
		}
	}

	@Override
	public void process(InputEvent event, InputController controller) {
		List<InputEventProcessor> inputProcessors = getInputProcessorMap().get(event.getInput());
		if (inputProcessors != null) {
			try {
				for (InputEventProcessor processor : inputProcessors) {
					processor.process(event, controller);
				}
			} catch (ConcurrentModificationException e) {
				// silently ignore (can happen when re-wiring joystick)
				// avoiding synchronized guard for performance and deadlock
			}
		}
	}

	@Override
	public void exit(InputController controller) {
		for (InputEventProcessor processor : getInputProcessors()) {
			processor.exit(controller);
		}
	}

	public Set<InputCommand> getInputProcessorCommands() {
		Set<InputCommand> commands = new HashSet<InputCommand>();
		for (Input input : getInputProcessorMap().keySet()) {
			for (InputEventProcessor processor : getInputProcessorMap().get(input)) {
				if (processor instanceof InputCommandProducer) {
					commands.add(((InputCommandProducer) processor).getCommand());
				}
			}
		}
		return commands;
	}

	private Set<InputEventProcessor> getInputProcessors() {
		Set<InputEventProcessor> processors = new HashSet<InputEventProcessor>();
		for (Input input : getInputProcessorMap().keySet()) {
			processors.addAll(getInputProcessorMap().get(input));
		}
		return processors;
	}

	public String getIdentifier() {
		return identifier;
	}

	private Map<Input, List<InputEventProcessor>> getInputProcessorMap() {
		return inputProcessorMap;
	}

}