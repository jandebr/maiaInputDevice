package org.maia.io.inputdevice.controller.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.maia.io.inputdevice.AnalogInputValueRange;
import org.maia.io.inputdevice.DigitalInputValueRange;
import org.maia.io.inputdevice.InputValueRange;
import org.maia.io.inputdevice.controller.GatedInputController;
import org.maia.io.inputdevice.controller.GatedInputSelector;
import org.maia.io.inputdevice.controller.InputCommand;
import org.maia.io.inputdevice.controller.InputControllerType;
import org.maia.io.inputdevice.controller.InputSelector;
import org.maia.io.inputdevice.joystick.JoystickCommand;

public class InputControllerConfigurationByPropertiesBuilder extends InputControllerConfigurationBuilder {

	private Properties properties;

	private String keyPrefix;

	private char keyComponentSeparator;

	private char valueComponentSeparator;

	public static char DEFAULT_KEYCOMP_SEPARATOR = '.';

	public static char DEFAULT_VALUECOMP_SEPARATOR = '|';

	private static final String KEYCOMP_TYPE = "type";

	private static final String KEYCOMP_NAME = "name";

	private static final String KEYCOMP_DEVICE_IDENTIFIER = "deviceIdentifier";

	private static final String KEYCOMP_CONTEXT = "context";

	private static final String KEYCOMP_COMMAND = "command";

	private static final String KEYCOMP_CONCURRENT_COMMANDS = "concurrentCommands";

	private static final String KEYCOMP_FAST_RELEASING = "fastReleasing";

	private static final String INPUT_TYPE_GATED = "GATED";

	private static final String INPUT_TYPE_STICK = "STICK";

	private static final String INPUT_TYPE_OTHER = "OTHER";

	public static Properties readPropertiesFromFile(File file) throws IOException {
		FileInputStream inStream = new FileInputStream(file);
		Properties properties = new Properties();
		properties.load(inStream);
		inStream.close();
		return properties;
	}

	public static void writePropertiesToFile(File file, Properties properties, String comment) throws IOException {
		FileOutputStream outStream = new FileOutputStream(file);
		properties.store(outStream, comment);
		outStream.close();
	}

	public InputControllerConfigurationByPropertiesBuilder(Properties properties) {
		this(properties, null);
	}

	public InputControllerConfigurationByPropertiesBuilder(Properties properties, String keyPrefix) {
		this(properties, keyPrefix, DEFAULT_KEYCOMP_SEPARATOR, DEFAULT_VALUECOMP_SEPARATOR);
	}

	public InputControllerConfigurationByPropertiesBuilder(Properties properties, String keyPrefix,
			char keyComponentSeparator, char valueComponentSeparator) {
		if (properties == null)
			throw new NullPointerException("Properties is null");
		this.properties = properties;
		this.keyPrefix = keyPrefix;
		this.keyComponentSeparator = keyComponentSeparator;
		this.valueComponentSeparator = valueComponentSeparator;
	}

	@Override
	public InputControllerConfiguration build() {
		InputControllerConfiguration configuration = null;
		Properties properties = getProperties();
		String controllerName = properties.getProperty(buildPropertyKey(KEYCOMP_NAME));
		String deviceIdentifier = properties.getProperty(buildPropertyKey(KEYCOMP_DEVICE_IDENTIFIER));
		if (controllerName != null && deviceIdentifier != null) {
			InputControllerType controllerType = InputControllerType.valueOf(properties
					.getProperty(buildPropertyKey(KEYCOMP_TYPE), getDefaultInputControllerType().name()).toUpperCase());
			configuration = new InputControllerConfiguration(controllerType, controllerName, deviceIdentifier);
			configuration.setConcurrentlyFiringCommandsEnabled(
					Boolean.parseBoolean(properties.getProperty(buildPropertyKey(KEYCOMP_CONCURRENT_COMMANDS),
							String.valueOf(InputControllerConfiguration.DEFAULT_CONCURRENT_COMMANDS))));
			configuration.setFastReleasing(
					Boolean.parseBoolean(properties.getProperty(buildPropertyKey(KEYCOMP_FAST_RELEASING),
							String.valueOf(InputControllerConfiguration.DEFAULT_FAST_RELEASING))));
			Map<String, InputControllerContextConfiguration> contextConfigurations = new HashMap<>();
			for (String key : properties.stringPropertyNames()) {
				if (isEmptyKeyPrefix() || key.startsWith(getKeyPrefix())) {
					String[] keyComponents = parsePropertyKey(key);
					if (keyComponents.length == 4 && keyComponents[0].equals(KEYCOMP_CONTEXT)
							&& keyComponents[2].equals(KEYCOMP_COMMAND)) {
						String contextId = keyComponents[1];
						String commandId = keyComponents[3];
						String valueString = properties.getProperty(key);
						InputControllerContextConfiguration contextConfiguration = contextConfigurations.get(contextId);
						if (contextConfiguration == null) {
							contextConfiguration = new InputControllerContextConfiguration(contextId);
							contextConfigurations.put(contextId, contextConfiguration);
							configuration.addContextConfiguration(contextConfiguration);
						}
						InputCommandConfiguration commandConfiguration = parseCommandConfiguration(valueString,
								commandId, deviceIdentifier);
						if (commandConfiguration != null) {
							contextConfiguration.addCommandConfiguration(commandConfiguration);
						}
					}
				}
			}
		}
		return configuration;
	}

	private InputCommandConfiguration parseCommandConfiguration(String valueString, String commandIdentifier,
			String deviceIdentifier) {
		InputCommandConfiguration commandConfiguration = null;
		int i = valueString.indexOf(getValueComponentSeparator());
		if (i < 0)
			i = valueString.length();
		String commandName = valueString.substring(0, i);
		int j = valueString.indexOf(getValueComponentSeparator(), i + 1);
		if (j >= 0) {
			String inputType = valueString.substring(i + 1, j).trim().toUpperCase();
			String inputSpecification = valueString.substring(j + 1);
			InputCommand command = parseInputCommand(inputType, commandName, commandIdentifier);
			InputSelector inputSelector = null;
			if (INPUT_TYPE_STICK.equals(inputType) || INPUT_TYPE_GATED.equals(inputType)) {
				inputSelector = parseGatedInputSelector(inputSpecification, deviceIdentifier);
			} else {
				inputSelector = parseInputSelector(inputSpecification, deviceIdentifier);
			}
			if (inputSelector != null) {
				commandConfiguration = new InputCommandConfiguration(command, inputSelector);
			}
		}
		return commandConfiguration;
	}

	private InputCommand parseInputCommand(String inputType, String commandName, String commandIdentifier) {
		if (INPUT_TYPE_STICK.equals(inputType)) {
			return new JoystickCommand(commandName, commandIdentifier);
		} else {
			return new InputCommand(commandName, commandIdentifier);
		}
	}

	private GatedInputSelector parseGatedInputSelector(String inputSpecification, String deviceIdentifier) {
		GatedInputSelector inputSelector = null;
		StringTokenizer st = new StringTokenizer(inputSpecification, String.valueOf(getValueComponentSeparator()));
		if (st.hasMoreTokens()) {
			String inputIdentifier = st.nextToken();
			if (st.hasMoreTokens()) {
				String inputName = st.nextToken();
				InputValueRange range = null;
				if (st.hasMoreTokens()) {
					float inputFiringMinValue = Float.parseFloat(st.nextToken());
					if (st.hasMoreTokens()) {
						float inputFiringMaxValue = Float.parseFloat(st.nextToken());
						range = new AnalogInputValueRange(inputFiringMinValue, inputFiringMaxValue);
					} else {
						range = new DigitalInputValueRange(inputFiringMinValue);
					}
				} else {
					range = new DigitalInputValueRange(GatedInputController.DEFAULT_INPUT_FIRING_VALUE);
				}
				inputSelector = new GatedInputSelector(deviceIdentifier, inputIdentifier, inputName, range);
			}
		}
		return inputSelector;
	}

	private InputSelector parseInputSelector(String inputSpecification, String deviceIdentifier) {
		InputSelector inputSelector = null;
		StringTokenizer st = new StringTokenizer(inputSpecification, String.valueOf(getValueComponentSeparator()));
		if (st.hasMoreTokens()) {
			String inputIdentifier = st.nextToken();
			if (st.hasMoreTokens()) {
				String inputName = st.nextToken();
				inputSelector = new InputSelector(deviceIdentifier, inputIdentifier, inputName);
			}
		}
		return inputSelector;
	}

	public void loadIntoProperties(InputControllerConfiguration configuration) {
		Properties properties = getProperties();
		properties.setProperty(buildPropertyKey(KEYCOMP_TYPE), configuration.getControllerType().name());
		properties.setProperty(buildPropertyKey(KEYCOMP_NAME), configuration.getControllerName());
		properties.setProperty(buildPropertyKey(KEYCOMP_DEVICE_IDENTIFIER), configuration.getDeviceIdentifier());
		properties.setProperty(buildPropertyKey(KEYCOMP_CONCURRENT_COMMANDS),
				String.valueOf(configuration.isConcurrentlyFiringCommandsEnabled()));
		properties.setProperty(buildPropertyKey(KEYCOMP_FAST_RELEASING),
				String.valueOf(configuration.isFastReleasing()));
		eraseCommandsInProperties(properties);
		for (InputControllerContextConfiguration contextConfiguration : configuration.getContextConfigurations()) {
			String contextId = contextConfiguration.getContextIdentifier();
			for (InputCommandConfiguration commandConfiguration : contextConfiguration.getCommandConfigurations()) {
				String commandId = commandConfiguration.getInputCommand().getIdentifier();
				String key = buildPropertyKey(KEYCOMP_CONTEXT, contextId, KEYCOMP_COMMAND, commandId);
				String value = formatCommandConfiguration(commandConfiguration);
				properties.setProperty(key, value);
			}
		}
	}

	private void eraseCommandsInProperties(Properties properties) {
		for (String key : properties.stringPropertyNames()) {
			if (isEmptyKeyPrefix() || key.startsWith(getKeyPrefix())) {
				String[] keyComponents = parsePropertyKey(key);
				if (keyComponents.length == 4 && keyComponents[0].equals(KEYCOMP_CONTEXT)
						&& keyComponents[2].equals(KEYCOMP_COMMAND)) {
					properties.remove(key);
				}
			}
		}
	}

	private String formatCommandConfiguration(InputCommandConfiguration commandConfiguration) {
		StringBuilder sb = new StringBuilder();
		sb.append(commandConfiguration.getInputCommand().getName());
		sb.append(getValueComponentSeparator());
		InputSelector inputSelector = commandConfiguration.getInputSelector();
		if (inputSelector instanceof GatedInputSelector) {
			if (commandConfiguration.getInputCommand() instanceof JoystickCommand) {
				sb.append(INPUT_TYPE_STICK);
			} else {
				sb.append(INPUT_TYPE_GATED);
			}
			sb.append(getValueComponentSeparator());
			sb.append(formatGatedInputSelector((GatedInputSelector) inputSelector));
		} else {
			sb.append(INPUT_TYPE_OTHER);
			sb.append(getValueComponentSeparator());
			sb.append(formatInputSelector(inputSelector));
		}
		return sb.toString();
	}

	private String formatGatedInputSelector(GatedInputSelector inputSelector) {
		StringBuilder sb = new StringBuilder(formatInputSelector(inputSelector));
		InputValueRange range = inputSelector.getInputFiringRange();
		if (range.isSingleValue()) {
			sb.append(getValueComponentSeparator());
			sb.append(range.getMinimumValue());
		} else {
			sb.append(getValueComponentSeparator());
			sb.append(range.getMinimumValue());
			sb.append(getValueComponentSeparator());
			sb.append(range.getMaximumValue());
		}
		return sb.toString();
	}

	private String formatInputSelector(InputSelector inputSelector) {
		if (inputSelector != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(inputSelector.getInputIdentifier());
			sb.append(getValueComponentSeparator());
			sb.append(inputSelector.getInputName());
			return sb.toString();
		} else {
			return "";
		}
	}

	private String buildPropertyKey(String... keyComponents) {
		StringBuilder sb = new StringBuilder();
		if (!isEmptyKeyPrefix()) {
			sb.append(getKeyPrefix());
		}
		for (int i = 0; i < keyComponents.length; i++) {
			if (i > 0 || !isEmptyKeyPrefix()) {
				sb.append(getKeyComponentSeparator());
			}
			sb.append(keyComponents[i]);
		}
		return sb.toString();
	}

	private String[] parsePropertyKey(String key) {
		if (!isEmptyKeyPrefix() && !key.startsWith(getKeyPrefix() + getKeyComponentSeparator()))
			return new String[0];
		List<String> keyComponents = new Vector<String>();
		String subKey = key.substring(isEmptyKeyPrefix() ? 0 : getKeyPrefix().length() + 1);
		StringTokenizer st = new StringTokenizer(subKey, String.valueOf(getKeyComponentSeparator()));
		while (st.hasMoreTokens()) {
			keyComponents.add(st.nextToken());
		}
		return keyComponents.toArray(new String[keyComponents.size()]);
	}

	public Properties getProperties() {
		return properties;
	}

	public boolean isEmptyKeyPrefix() {
		return getKeyPrefix() == null || getKeyPrefix().isEmpty();
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	private char getKeyComponentSeparator() {
		return keyComponentSeparator;
	}

	private char getValueComponentSeparator() {
		return valueComponentSeparator;
	}

}