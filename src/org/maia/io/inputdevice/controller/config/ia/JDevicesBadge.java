package org.maia.io.inputdevice.controller.config.ia;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.maia.io.inputdevice.InputDevice;
import org.maia.io.inputdevice.InputDeviceFilter;
import org.maia.io.inputdevice.InputEventGateway;

@SuppressWarnings("serial")
public class JDevicesBadge extends JButton {

	private InputDeviceFilter deviceFilter;

	private InputDevice selectedDevice;

	private ToolTipController toolTipController;

	private boolean showDevicesExcludedByFilter;

	private int deviceCount;

	public JDevicesBadge() {
		this(InputDeviceFilter.ACCEPT_ALL);
	}

	public JDevicesBadge(InputDeviceFilter deviceFilter) {
		super(JInteractiveBuilderIcons.controllerIcon);
		this.deviceFilter = deviceFilter;
		this.toolTipController = new ToolTipController();
		updateDevices();
		setContentAreaFilled(false);
		setFocusPainted(false);
		setBorder(new DeviceCountBorder());
		addMouseListener(getToolTipController());
	}

	public void refresh() {
		getToolTipController().hideToolTip();
		updateDevices();
		repaint();
	}

	public void dispose() {
		getToolTipController().hideToolTip();
	}

	private void updateDevices() {
		int n = 0;
		ToolTip toolTip = getToolTipController().getToolTip();
		toolTip.clear();
		InputDeviceFilter filter = getDeviceFilter();
		for (InputDevice device : InputEventGateway.getInstance().getInputDevices()) {
			if (filter == null || filter.accept(device)) {
				toolTip.addDevice(device, true);
				n++;
			} else if (isShowDevicesExcludedByFilter()) {
				toolTip.addDevice(device, false);
			}
		}
		setDeviceCount(n);
	}

	public InputDeviceFilter getDeviceFilter() {
		return deviceFilter;
	}

	public InputDevice getSelectedDevice() {
		return selectedDevice;
	}

	public void setSelectedDevice(InputDevice selectedDevice) {
		this.selectedDevice = selectedDevice;
	}

	private ToolTipController getToolTipController() {
		return toolTipController;
	}

	public boolean isShowDevicesExcludedByFilter() {
		return showDevicesExcludedByFilter;
	}

	public void setShowDevicesExcludedByFilter(boolean show) {
		this.showDevicesExcludedByFilter = show;
	}

	public int getDeviceCount() {
		return deviceCount;
	}

	private void setDeviceCount(int deviceCount) {
		this.deviceCount = deviceCount;
	}

	private class DeviceCountBorder implements Border {

		private Color backgroundColor = new Color(93, 45, 166, 180);

		private Color foregroundColor = Color.WHITE;

		private static final int INSET_RIGHT = 6;

		public DeviceCountBorder() {
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics g2 = g.create();
			g2.setFont(g2.getFont().deriveFont(10f));
			String countStr = String.valueOf(getDeviceCount());
			FontMetrics fm = g2.getFontMetrics();
			int xmargin = 3, ymargin = 1;
			int cw = fm.stringWidth(countStr) + 2 * xmargin;
			int ch = fm.getAscent() - 1 + 2 * ymargin;
			int x0 = x + width - cw;
			int x1 = x + width - 1;
			int y0 = y;
			int y1 = y0 + ch - 1;
			g2.setColor(backgroundColor);
			g2.drawLine(x0, y0 + 3, x0, y1 - 3);
			g2.drawLine(x0 + 1, y0 + 1, x0 + 1, y1 - 1);
			g2.fillRect(x0 + 2, y0, cw - 4, ch);
			g2.drawLine(x1 - 1, y0 + 1, x1 - 1, y1 - 1);
			g2.drawLine(x1, y0 + 3, x1, y1 - 3);
			g2.setColor(foregroundColor);
			g2.drawString(countStr, x0 + xmargin, y1 - ymargin);
			g2.dispose();
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return new Insets(0, 0, 0, INSET_RIGHT);
		}

		@Override
		public boolean isBorderOpaque() {
			return false;
		}

	}

	private class ToolTipController extends MouseAdapter {

		private JWindow toolTipWindow;

		private ToolTip toolTip;

		public ToolTipController() {
			this.toolTip = new ToolTip();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			showToolTip();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			hideToolTip();
		}

		public void showToolTip() {
			hideToolTip();
			setToolTipWindow(createToolTipWindow());
			JWindow window = getToolTipWindow();
			if (window != null) {
				window.setVisible(true);
			}
		}

		public void hideToolTip() {
			JWindow window = getToolTipWindow();
			if (window != null) {
				window.setVisible(false);
				window.dispose();
				setToolTipWindow(null);
			}
		}

		private JWindow createToolTipWindow() {
			JWindow window = null;
			if (!getToolTip().isEmpty()) {
				JDevicesBadge badge = getBadge();
				Point badgeLoc = badge.getLocationOnScreen();
				window = new JWindow(SwingUtilities.getWindowAncestor(badge));
				window.add(getToolTip());
				window.setLocation(badgeLoc.x, badgeLoc.y + badge.getHeight());
				window.pack();
			}
			return window;
		}

		private JDevicesBadge getBadge() {
			return JDevicesBadge.this;
		}

		private JWindow getToolTipWindow() {
			return toolTipWindow;
		}

		private void setToolTipWindow(JWindow window) {
			this.toolTipWindow = window;
		}

		public ToolTip getToolTip() {
			return toolTip;
		}

	}

	private class ToolTip extends Box {

		private Color backgroundColor = new Color(246, 242, 252);

		private Color backgroundSelectedColor = new Color(240, 238, 146);

		private Color outlineColor = new Color(160, 161, 153);

		private Color foregroundEnabledColor = new Color(50, 50, 50);

		private Color foregroundDisabledColor = new Color(50, 50, 50, 100);

		public ToolTip() {
			super(BoxLayout.Y_AXIS);
			setOpaque(true);
			setBackground(backgroundColor);
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(outlineColor),
					BorderFactory.createEmptyBorder(4, 4, 4, 4)));
		}

		public void clear() {
			removeAll();
		}

		public void addDevice(InputDevice device, boolean enabled) {
			add(createDeviceToolTipComponent(device, enabled));
		}

		private JComponent createDeviceToolTipComponent(InputDevice device, boolean enabled) {
			Box box = Box.createVerticalBox();
			box.add(createDeviceNameLabel(device, enabled));
			box.add(createDeviceInfoLabel(device, enabled));
			if (!isEmpty()) {
				box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 0, 1, 0),
						BorderFactory.createMatteBorder(1, 0, 0, 0, outlineColor)));
			}
			Box compBox = Box.createHorizontalBox();
			compBox.add(new JLabel(getDeviceTypeIcon(device, enabled)));
			compBox.add(Box.createHorizontalStrut(6));
			compBox.add(box);
			compBox.add(Box.createHorizontalGlue());
			if (device.equals(getSelectedDevice())) {
				compBox.setOpaque(true);
				compBox.setBackground(backgroundSelectedColor);
			}
			return compBox;
		}

		private JLabel createDeviceNameLabel(InputDevice device, boolean enabled) {
			JLabel label = new JLabel(device.getName());
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			label.setForeground(enabled ? foregroundEnabledColor : foregroundDisabledColor);
			return label;
		}

		private JLabel createDeviceInfoLabel(InputDevice device, boolean enabled) {
			JLabel label = new JLabel(device.getTypeString() + ", " + device.getInputs().size() + " inputs");
			label.setFont(label.getFont().deriveFont(10f).deriveFont(Font.ITALIC));
			label.setForeground(enabled ? foregroundEnabledColor : foregroundDisabledColor);
			return label;
		}

		private ImageIcon getDeviceTypeIcon(InputDevice device, boolean enabled) {
			if (device.isTypeKeyboard()) {
				return enabled ? JInteractiveBuilderIcons.typeKeyboardIcon
						: JInteractiveBuilderIcons.typeKeyboardDisabledIcon;
			} else if (device.isTypeMouse()) {
				return enabled ? JInteractiveBuilderIcons.typeMouseIcon
						: JInteractiveBuilderIcons.typeMouseDisabledIcon;
			} else if (device.isTypeGamepad()) {
				return enabled ? JInteractiveBuilderIcons.typeGamepadIcon
						: JInteractiveBuilderIcons.typeGamepadDisabledIcon;
			} else if (device.isTypeStick()) {
				return enabled ? JInteractiveBuilderIcons.typeStickIcon
						: JInteractiveBuilderIcons.typeStickDisabledIcon;
			} else if (device.isTypeUnknown()) {
				return enabled ? JInteractiveBuilderIcons.typeUnknownIcon
						: JInteractiveBuilderIcons.typeUnknownDisabledIcon;
			} else {
				return enabled ? JInteractiveBuilderIcons.typeOtherIcon
						: JInteractiveBuilderIcons.typeOtherDisabledIcon;
			}
		}

		public boolean isEmpty() {
			return getComponentCount() == 0;
		}

	}

}