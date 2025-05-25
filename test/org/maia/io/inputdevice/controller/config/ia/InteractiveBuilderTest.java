package org.maia.io.inputdevice.controller.config.ia;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.maia.io.inputdevice.InputDeviceFilter;
import org.maia.io.inputdevice.Test;
import org.maia.io.inputdevice.controller.InputCommand;
import org.maia.io.inputdevice.controller.InputCommandGroup;
import org.maia.io.inputdevice.controller.InputControllerException;
import org.maia.io.inputdevice.controller.InputControllerType;
import org.maia.io.inputdevice.controller.config.InputControllerByConfigurationBuilder;
import org.maia.io.inputdevice.controller.config.InputControllerConfiguration;
import org.maia.io.inputdevice.controller.config.InputControllerConfigurationByPropertiesBuilder;
import org.maia.io.inputdevice.joystick.Joystick;
import org.maia.io.inputdevice.joystick.JoystickCommand;
import org.maia.io.inputdevice.joystick.JoystickListener;
import org.maia.util.SystemUtils;

public class InteractiveBuilderTest extends Test implements JInteractiveBuilderListener, JoystickListener {

	private JFrame frame;

	private Joystick joystick;

	private File controllerPropertiesFile = new File("testcontroller.config");

	private static final String PROPERTIES_KEY_PREFIX = "testcontroller";

	private static final String CONTEXT_MENU = "Menu";

	private static final String CONTEXT_GAME = "Game";

	private static final JoystickCommand COMMAND_LEFT = new JoystickCommand("Left");

	private static final JoystickCommand COMMAND_RIGHT = new JoystickCommand("Right");

	private static final JoystickCommand COMMAND_UP = new JoystickCommand("Up");

	private static final JoystickCommand COMMAND_DOWN = new JoystickCommand("Down");

	private static final JoystickCommand COMMAND_CLEAR = new JoystickCommand("Clear");

	private static final JoystickCommand COMMAND_ENTER = new JoystickCommand("Enter");

	private static final JoystickCommand COMMAND_FIRE = new JoystickCommand("Fire");

	static {
		COMMAND_CLEAR.setDescription("Erases an item");
		COMMAND_UP.setDescription(
				"A game controller, gaming controller, or simply controller, is an input device or input/output device used with video games or entertainment systems to provide input to a video game. Input devices that have been classified as game controllers include keyboards, mice, gamepads, and joysticks, as well as special purpose devices, such as steering wheels for driving games and light guns for shooting games.");
	}

	public static void main(String[] args) {
		new InteractiveBuilderTest().startTest();
	}

	@Override
	public void startTest() {
		renewJoystick();
		InteractiveBuilder builder = createBuilder();
		setFrame(showFrame("Interactive test", createSetupPanel(builder)));
	}

	private JPanel createSetupPanel(InteractiveBuilder builder) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(createSetupButton(builder), BorderLayout.CENTER);
		panel.add(createInfoLabel(), BorderLayout.SOUTH);
		return panel;
	}

	private JLabel createInfoLabel() {
		final JLabel label = new JLabel("Info");
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				Runtime rt = Runtime.getRuntime();
				do {
					long usedMemory = rt.totalMemory() - rt.freeMemory();
					int usedMemoryMB = (int) Math.floorDiv(usedMemory, 1000000);
					label.setText("Memory usage: " + usedMemoryMB + " MB");
					SystemUtils.sleep(100L);
				} while (true);
			}
		});
		t.setDaemon(true);
		t.start();
		return label;
	}

	private JButton createSetupButton(final InteractiveBuilder builder) {
		JButton button = new JButton("Setup controller...");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				pauseJoystick();
				builder.resetTo(importConfigFromPropertiesFile());
				JInteractiveBuilder jbuilder = new JInteractiveBuilder(builder, getFrame(), "Setup controller");
				jbuilder.addListener(InteractiveBuilderTest.this);
				jbuilder.show();
			}
		});
		button.setPreferredSize(new Dimension(300, 200));
		return button;
	}

	private InteractiveBuilder createBuilder() {
		List<InputCommandGroup> commandGroups = createCommandGroups();
		final Set<InputCommand> requiredCommands = getRequiredCommands();
		InteractiveBuilder builder = new InteractiveBuilder(commandGroups, InputDeviceFilter.STICK_OR_GAMEPAD);
		builder.withControllerType(InputControllerType.JOYSTICK).withControllerName("Joystick-1")
				.withControls(new InteractiveBuilderControls() {

					@Override
					public InteractiveBuilderControlType getType(InputCommand command) {
						if (COMMAND_UP.equals(command)) {
							return InteractiveBuilderControlType.PREVIOUS;
						} else if (COMMAND_DOWN.equals(command)) {
							return InteractiveBuilderControlType.NEXT;
						} else if (COMMAND_CLEAR.equals(command)) {
							return InteractiveBuilderControlType.CLEAR;
						} else if (COMMAND_ENTER.equals(command)) {
							return InteractiveBuilderControlType.SUBMIT;
						} else {
							return null;
						}
					}

				}).withRequiredCommands(new RequiredInputCommands() {

					@Override
					public boolean isRequired(InputCommand command) {
						return requiredCommands.contains(command);
					}
				});
		return builder;
	}

	private List<InputCommandGroup> createCommandGroups() {
		List<InputCommandGroup> commandGroups = new Vector<InputCommandGroup>();
		InputCommandGroup group = new InputCommandGroup(CONTEXT_MENU);
		group.addMember(COMMAND_CLEAR);
		group.addMember(COMMAND_UP);
		group.addMember(COMMAND_DOWN);
		group.addMember(COMMAND_LEFT);
		group.addMember(COMMAND_RIGHT);
		group.addMember(COMMAND_ENTER);
		commandGroups.add(group);
		group = new InputCommandGroup(CONTEXT_GAME);
		group.addMember(COMMAND_UP);
		group.addMember(COMMAND_DOWN);
		group.addMember(COMMAND_LEFT);
		group.addMember(COMMAND_RIGHT);
		group.addMember(COMMAND_FIRE);
		commandGroups.add(group);
		return commandGroups;
	}

	private Set<InputCommand> getRequiredCommands() {
		Set<InputCommand> commands = new HashSet<InputCommand>();
		commands.add(COMMAND_UP);
		commands.add(COMMAND_DOWN);
		commands.add(COMMAND_LEFT);
		commands.add(COMMAND_RIGHT);
		commands.add(COMMAND_FIRE);
		return commands;
	}

	private InputControllerConfiguration importConfigFromPropertiesFile() {
		return importConfigFromPropertiesFile(getControllerPropertiesFile(), PROPERTIES_KEY_PREFIX);
	}

	private InputControllerConfiguration importConfigFromPropertiesFile(File file, String keyPrefix) {
		InputControllerConfiguration cfg = null;
		try {
			Properties props = InputControllerConfigurationByPropertiesBuilder.readPropertiesFromFile(file);
			cfg = new InputControllerConfigurationByPropertiesBuilder(props, keyPrefix).build();
		} catch (IOException e) {
			// file may not exist
		}
		return cfg;
	}

	private void exportConfigToPropertiesFile(InputControllerConfiguration configuration) {
		exportConfigToPropertiesFile(configuration, getControllerPropertiesFile(), PROPERTIES_KEY_PREFIX);
	}

	private void exportConfigToPropertiesFile(InputControllerConfiguration configuration, File file, String keyPrefix) {
		Properties props = new Properties();
		new InputControllerConfigurationByPropertiesBuilder(props, keyPrefix).loadIntoProperties(configuration);
		try {
			InputControllerConfigurationByPropertiesBuilder.writePropertiesToFile(file, props, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void interactiveBuilderCancelled(JInteractiveBuilder jbuilder) {
		resumeJoystick();
	}

	@Override
	public void interactiveBuilderCompleted(JInteractiveBuilder jbuilder) {
		InputControllerConfiguration configuration = jbuilder.getConfiguration();
		exportConfigToPropertiesFile(configuration);
		renewJoystick(configuration);
	}

	private void renewJoystick() {
		renewJoystick(importConfigFromPropertiesFile());
	}

	private synchronized void renewJoystick(InputControllerConfiguration configuration) {
		Joystick joystick = getJoystick();
		if (joystick != null) {
			System.out.println("Disposing joystick");
			joystick.removeJoystickListener(this);
			joystick.dispose();
			setJoystick(null);
		}
		joystick = createJoystick(configuration);
		if (joystick != null) {
			System.out.println("Activating new joystick");
			setJoystick(joystick);
			joystick.addJoystickListener(this);
		}
	}

	private Joystick createJoystick(InputControllerConfiguration configuration) {
		Joystick joystick = null;
		if (configuration != null) {
			try {
				joystick = new InputControllerByConfigurationBuilder(configuration).buildJoystick();
				joystick.switchContext(CONTEXT_GAME);
				joystick.setAutoRepeatEnabled(true);
			} catch (InputControllerException e) {
				// input device may no longer be connected
			}
		}
		return joystick;
	}

	private void pauseJoystick() {
		Joystick joystick = getJoystick();
		if (joystick != null) {
			joystick.setActive(false);
		}
	}

	private void resumeJoystick() {
		Joystick joystick = getJoystick();
		if (joystick != null) {
			joystick.setActive(true);
		}
	}

	@Override
	public void joystickCommandFired(Joystick joystick, JoystickCommand command, boolean autoRepeat) {
		System.out.println(joystick.getName() + " fired " + command.getName() + (autoRepeat ? " (auto-repeat)" : ""));
	}

	@Override
	public void joystickCommandReleased(Joystick joystick, JoystickCommand command) {
		System.out.println(joystick.getName() + " released " + command.getName());
	}

	private JFrame getFrame() {
		return frame;
	}

	private void setFrame(JFrame frame) {
		this.frame = frame;
	}

	private Joystick getJoystick() {
		return joystick;
	}

	private void setJoystick(Joystick joystick) {
		this.joystick = joystick;
	}

	private File getControllerPropertiesFile() {
		return controllerPropertiesFile;
	}

}