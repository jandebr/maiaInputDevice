package org.maia.io.inputdevice.controller.config.ia;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.maia.io.inputdevice.AnalogInputSensitive;
import org.maia.io.inputdevice.controller.config.InputControllerConfiguration;
import org.maia.swing.dialog.ActionableDialog;
import org.maia.swing.dialog.ActionableDialogAdapter;
import org.maia.swing.dialog.ActionableDialogButton;
import org.maia.swing.dialog.ActionableDialogOption;
import org.maia.util.GenericListenerList;

public class JInteractiveBuilder extends ActionableDialogAdapter implements KeyListener {

	private InteractiveBuilder builder;

	private JInteractiveBuilderPanel panel;

	private JButton refreshDevicesButton;

	private JButton bluetoothButton;

	private JDevicesBadge devicesBadge;

	private JSlider sensitivitySlider;

	private JCheckBox concurrentCommandsCheckBox;

	private JCheckBox fastReleasingCheckBox;

	private ActionableDialog dialog;

	private ActionableDialogButton confirmationButton;

	private ActionableDialogButton resetButton;

	private BluetoothSettingsCallout bluetoothCallout;

	private GenericListenerList<JInteractiveBuilderListener> listeners;

	private boolean keyboardTraversalEnabled;

	private boolean closed;

	private boolean fired;

	private static final ActionableDialogOption RESET_OPTION = new ResetOption();

	public static int SENSITIVITY_DISCRETE_VALUES = 100;

	public static float SENSITIVITY_LOWER_BOUND_MARGIN = 0.1f;

	public static float SENSITIVITY_UPPER_BOUND_MARGIN = 0.1f;

	public JInteractiveBuilder(InteractiveBuilder builder, Window windowOwner, String windowTitle) {
		this.builder = builder;
		this.panel = createPanel();
		this.refreshDevicesButton = createRefreshDevicesButton();
		this.bluetoothButton = createBluetoothButton();
		this.devicesBadge = new JDevicesBadge(builder.getAcceptedDevices());
		this.sensitivitySlider = createSensitivitySlider();
		this.concurrentCommandsCheckBox = createConcurrentCommandsCheckBox();
		this.fastReleasingCheckBox = createFastReleasingCheckBox();
		this.dialog = createDialog(windowOwner, windowTitle);
		this.listeners = new GenericListenerList<JInteractiveBuilderListener>();
		getPanel().addKeyListener(this);
		getConfirmationButton().addKeyListener(this);
		setKeyboardTraversalEnabled(true);
		setBluetoothCallout(new BluetoothSettingsCalloutExecutable());
	}

	private JInteractiveBuilderPanel createPanel() {
		JInteractiveBuilderPanel panel = new JInteractiveBuilderPanel(this);
		getBuilder().addListener(panel);
		return panel;
	}

	private ActionableDialog createDialog(Window windowOwner, String windowTitle) {
		List<ActionableDialogOption> dialogOptions = new Vector<ActionableDialogOption>(3);
		dialogOptions.add(ActionableDialog.OK_OPTION);
		dialogOptions.add(RESET_OPTION);
		dialogOptions.add(ActionableDialog.CANCEL_OPTION);
		ActionableDialog dialog = new ActionableDialog(windowOwner, windowTitle, true, createDialogMainComponent(),
				dialogOptions);
		dialog.addListener(this);
		dialog.center();
		dialog.fitInScreen();
		return dialog;
	}

	private JComponent createDialogMainComponent() {
		JPanel comp = new JPanel(new BorderLayout(0, 8));
		comp.add(createControlPanel(), BorderLayout.NORTH);
		comp.add(getPanel(), BorderLayout.CENTER);
		return comp;
	}

	private JComponent createControlPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		c.gridy = 0;
		c.gridx = 0;
		panel.add(createToolbarPanel(), c);
		c.insets = new Insets(8, 0, 0, 0);
		c.gridy = 1;
		c.gridx = 0;
		panel.add(createSettingsPanel(), c);
		return panel;
	}

	private JComponent createToolbarPanel() {
		Box box = Box.createHorizontalBox();
		box.add(Box.createHorizontalStrut(4));
		box.add(getBluetoothButton());
		box.add(Box.createHorizontalStrut(16));
		box.add(getRefreshDevicesButton());
		box.add(Box.createHorizontalStrut(64));
		box.add(Box.createHorizontalGlue());
		box.add(getDevicesBadge());
		box.add(Box.createHorizontalStrut(4));
		return box;
	}

	private JComponent createSettingsPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		c.gridy = 0;
		c.gridx = 0;
		panel.add(getConcurrentCommandsCheckBox(), c);
		c.gridy = 1;
		c.gridx = 0;
		panel.add(getFastReleasingCheckBox(), c);
		c.gridy = 2;
		c.gridx = 0;
		c.insets = new Insets(4, 4, 4, 4);
		panel.add(createSensitivityComponent(), c);
		return panel;
	}

	private JComponent createSensitivityComponent() {
		Box box = Box.createHorizontalBox();
		box.add(new JLabel("Sensitivity"));
		box.add(Box.createHorizontalStrut(4));
		box.add(getSensitivitySlider());
		box.add(Box.createHorizontalGlue());
		return box;
	}

	private JSlider createSensitivitySlider() {
		int value = projectSensitivityToSliderValue(getBuilder().getAnalogSensitivity());
		JSlider slider = new JSlider(0, SENSITIVITY_DISCRETE_VALUES - 1, value);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider) e.getSource();
				if (!slider.getValueIsAdjusting()) {
					doChangeAnalogSensitivity(projectSliderValueToSensitivity(slider.getValue()));
				}
			}
		});
		return slider;
	}

	private JCheckBox createConcurrentCommandsCheckBox() {
		final JCheckBox checkBox = new JCheckBox("Concurrent controls",
				getBuilder().isConcurrentlyFiringCommandsEnabled());
		checkBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				doSetConcurrentlyFiringCommandsEnabled(checkBox.isSelected());
			}
		});
		return checkBox;
	}

	private JCheckBox createFastReleasingCheckBox() {
		final JCheckBox checkBox = new JCheckBox("Fast releasing", getBuilder().isFastReleasing());
		checkBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				doSetFastReleasing(checkBox.isSelected());
			}
		});
		return checkBox;
	}

	private JButton createBluetoothButton() {
		JButton button = new JButton("Bluetooth...", JInteractiveBuilderIcons.bluetoothIcon);
		button.setToolTipText("Manage Bluetooth devices");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				doBluetoothCallout();
			}
		});
		return button;
	}

	private JButton createRefreshDevicesButton() {
		JButton button = new JButton("Devices", JInteractiveBuilderIcons.refreshIcon);
		button.setToolTipText("Refresh connected devices");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						doRefreshDevices();
					}
				}).start();
			}
		});
		return button;
	}

	@Override
	public void dialogButtonClicked(ActionableDialog dialog, ActionableDialogOption dialogOption) {
		if (dialogOption.equals(RESET_OPTION)) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					doReset();
				}
			}).start();
		}
	}

	public void doReset() {
		getBuilder().reset();
	}

	public void doBluetoothCallout() {
		BluetoothSettingsCallout callout = getBluetoothCallout();
		if (callout != null && callout.canCallout()) {
			callout.callout();
		}
	}

	public void doRefreshDevices() {
		getBuilder().scanInputDevices();
		doRefreshDevicesBadge();
	}

	void doRefreshDevicesBadge() {
		getDevicesBadge().setSelectedDevice(getBuilder().getDevice());
		getDevicesBadge().refresh();
	}

	public void doClickConfirmationButton() {
		ActionableDialogButton button = getConfirmationButton();
		if (button.isEnabled()) {
			button.doClick();
		}
	}

	public void doChangeAnalogSensitivity(float sensitivity) {
		getBuilder().changeAnalogSensitivity(sensitivity);
		getSensitivitySlider().setValue(projectSensitivityToSliderValue(sensitivity));
	}

	public void doSetConcurrentlyFiringCommandsEnabled(boolean enabled) {
		getBuilder().setConcurrentlyFiringCommandsEnabled(enabled);
		getConcurrentCommandsCheckBox().setSelected(enabled);
	}

	public void doSetFastReleasing(boolean fastReleasing) {
		getBuilder().setFastReleasing(fastReleasing);
		getFastReleasingCheckBox().setSelected(fastReleasing);
	}

	public void addListener(JInteractiveBuilderListener listener) {
		getListeners().addListener(listener);
	}

	public void removeListener(JInteractiveBuilderListener listener) {
		getListeners().removeListener(listener);
	}

	public void show() {
		if (isClosed())
			throw new IllegalStateException("Builder dialog cannot be re-opened. Should create a new dialog");
		getDialog().setVisible(true);
		getBuilder().startInteraction();
		doRefreshDevicesBadge();
	}

	@Override
	public void dialogConfirmed(ActionableDialog dialog) {
		fireCompleted();
	}

	@Override
	public void dialogCancelled(ActionableDialog dialog) {
		fireCancelled();
	}

	@Override
	public void dialogClosed(ActionableDialog dialog) {
		setClosed(true);
		getBuilder().stopInteraction();
		getBuilder().removeListener(getPanel());
		getPanel().removeKeyListener(this);
		getConfirmationButton().removeKeyListener(this);
		getDevicesBadge().dispose();
		if (!isFired()) {
			fireCancelled();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// no action
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (isKeyboardTraversalEnabled()) {
			int code = e.getKeyCode();
			if (code == KeyEvent.VK_UP || code == KeyEvent.VK_KP_UP) {
				getBuilder().rollToPreviousCommand();
			} else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_KP_DOWN) {
				getBuilder().rollToNextCommand();
			} else if (code == KeyEvent.VK_HOME) {
				getBuilder().moveToFirstCommand();
			} else if (code == KeyEvent.VK_END) {
				getBuilder().moveToLastCommand();
			} else if (code == KeyEvent.VK_DELETE) {
				getBuilder().clearCurrentCommandAssignment();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// no action
	}

	protected void refreshBluetoothCalloutButton() {
		BluetoothSettingsCallout callout = getBluetoothCallout();
		getBluetoothButton().setEnabled(callout != null && callout.canCallout());
	}

	protected void fireCompleted() {
		setFired(true);
		for (JInteractiveBuilderListener listener : getListeners()) {
			listener.interactiveBuilderCompleted(this);
		}
	}

	protected void fireCancelled() {
		setFired(true);
		for (JInteractiveBuilderListener listener : getListeners()) {
			listener.interactiveBuilderCancelled(this);
		}
	}

	private int projectSensitivityToSliderValue(float sensitivity) {
		float min = getMinimumSensitivityInclusive();
		float max = getMaximumSensitivityExclusive();
		int value = (int) Math.floor(SENSITIVITY_DISCRETE_VALUES * (sensitivity - min) / (max - min));
		return Math.max(Math.min(value, SENSITIVITY_DISCRETE_VALUES - 1), 0);
	}

	private float projectSliderValueToSensitivity(int value) {
		float min = getMinimumSensitivityInclusive();
		float max = getMaximumSensitivityExclusive();
		float r = value / (float) SENSITIVITY_DISCRETE_VALUES;
		return (1.0f - r) * min + r * max;
	}

	private float getMinimumSensitivityInclusive() {
		return AnalogInputSensitive.MINIMUM_SENSITIVITY_INCLUSIVE + SENSITIVITY_LOWER_BOUND_MARGIN;
	}

	private float getMaximumSensitivityExclusive() {
		return AnalogInputSensitive.MAXIMUM_SENSITIVITY_EXCLUSIVE - SENSITIVITY_UPPER_BOUND_MARGIN;
	}

	public InputControllerConfiguration getConfiguration() {
		return getBuilder().build();
	}

	ActionableDialogButton getConfirmationButton() {
		if (confirmationButton == null) {
			for (ActionableDialogButton button : getDialog().getDialogButtons()) {
				if (ActionableDialog.OK_OPTION.equals(button.getOption())) {
					confirmationButton = button;
				}
			}
		}
		return confirmationButton;
	}

	ActionableDialogButton getResetButton() {
		if (resetButton == null) {
			for (ActionableDialogButton button : getDialog().getDialogButtons()) {
				if (RESET_OPTION.equals(button.getOption())) {
					resetButton = button;
				}
			}
		}
		return resetButton;
	}

	InteractiveBuilder getBuilder() {
		return builder;
	}

	public boolean isShowCommandGroupNames() {
		return getPanel().isShowCommandGroupNames();
	}

	public void setShowCommandGroupNames(boolean show) {
		getPanel().setShowCommandGroupNames(show);
	}

	public boolean isShowCommandDescriptions() {
		return getPanel().isShowCommandDescriptions();
	}

	public void setShowCommandDescriptions(boolean show) {
		getPanel().setShowCommandDescriptions(show);
	}

	private JInteractiveBuilderPanel getPanel() {
		return panel;
	}

	private JButton getRefreshDevicesButton() {
		return refreshDevicesButton;
	}

	private JButton getBluetoothButton() {
		return bluetoothButton;
	}

	private JDevicesBadge getDevicesBadge() {
		return devicesBadge;
	}

	private JSlider getSensitivitySlider() {
		return sensitivitySlider;
	}

	private JCheckBox getConcurrentCommandsCheckBox() {
		return concurrentCommandsCheckBox;
	}

	private JCheckBox getFastReleasingCheckBox() {
		return fastReleasingCheckBox;
	}

	public ActionableDialog getDialog() {
		return dialog;
	}

	public BluetoothSettingsCallout getBluetoothCallout() {
		return bluetoothCallout;
	}

	public void setBluetoothCallout(BluetoothSettingsCallout callout) {
		this.bluetoothCallout = callout;
		refreshBluetoothCalloutButton();
	}

	private GenericListenerList<JInteractiveBuilderListener> getListeners() {
		return listeners;
	}

	public boolean isKeyboardTraversalEnabled() {
		return keyboardTraversalEnabled;
	}

	public void setKeyboardTraversalEnabled(boolean enabled) {
		this.keyboardTraversalEnabled = enabled;
	}

	private boolean isClosed() {
		return closed;
	}

	private void setClosed(boolean closed) {
		this.closed = closed;
	}

	private boolean isFired() {
		return fired;
	}

	private void setFired(boolean fired) {
		this.fired = fired;
	}

	private static class ResetOption extends ActionableDialogOption {

		public ResetOption() {
			super("RESET", "Reset");
		}

		@Override
		public boolean isConfirmation() {
			return false;
		}

		@Override
		public boolean isCancellation() {
			return false;
		}

		@Override
		public boolean isClosingDialog() {
			return false;
		}

	}

}