package org.maia.io.inputdevice.controller.config;

import java.util.HashSet;
import java.util.Set;

import org.maia.io.inputdevice.controller.InputControllerType;

public class InputControllerConfiguration {

	private InputControllerType controllerType;

	private String controllerName;

	private String deviceIdentifier;

	private Set<InputControllerContextConfiguration> contextConfigurations;

	private boolean concurrentlyFiringCommandsEnabled; // ability to fire different commands concurrently

	private boolean fastReleasing; // ability to preemptively release at decline of an input's value

	public static boolean DEFAULT_CONCURRENT_COMMANDS = true;

	public static boolean DEFAULT_FAST_RELEASING = true;

	public InputControllerConfiguration(InputControllerType controllerType, String controllerName,
			String deviceIdentifier) {
		this.controllerType = controllerType;
		this.controllerName = controllerName;
		this.deviceIdentifier = deviceIdentifier;
		this.contextConfigurations = new HashSet<InputControllerContextConfiguration>();
		this.concurrentlyFiringCommandsEnabled = DEFAULT_CONCURRENT_COMMANDS;
		this.fastReleasing = DEFAULT_FAST_RELEASING;
	}

	public void addContextConfiguration(InputControllerContextConfiguration contextConfiguration) {
		getContextConfigurations().add(contextConfiguration);
	}

	public boolean hasContextConfigurations() {
		return !getContextConfigurations().isEmpty();
	}

	public InputControllerType getControllerType() {
		return controllerType;
	}

	public String getControllerName() {
		return controllerName;
	}

	public String getDeviceIdentifier() {
		return deviceIdentifier;
	}

	public Set<InputControllerContextConfiguration> getContextConfigurations() {
		return contextConfigurations;
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

}