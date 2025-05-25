package org.maia.io.inputdevice.controller.config.ia;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.maia.io.inputdevice.controller.GatedInputSelector;
import org.maia.io.inputdevice.controller.InputCommand;
import org.maia.io.inputdevice.controller.InputCommandGroup;
import org.maia.swing.dialog.ActionableDialogButton;

@SuppressWarnings("serial")
public class JInteractiveBuilderPanel extends JPanel implements InteractiveBuilderListener {

	private JInteractiveBuilder builderWidget;

	private List<JCommandEntry> commandEntries;

	private JScrollPane commandScrollPane;

	private boolean showCommandGroupNames;

	private boolean showCommandDescriptions;

	public static Color CURRENT_COMMAND_BACKGROUND = new Color(211, 230, 181); // very light green

	public static Color CURRENT_COMMAND_BORDER_COLOR = new Color(201, 219, 175); // light green

	public static Color CURRENT_COMMAND_COLOR_WHEN_ASSIGNED = new Color(26, 66, 12); // dark green

	public static Color CURRENT_COMMAND_COLOR_WHEN_UNASSIGNED = new Color(6, 40, 64); // dark blue

	public static Color OTHER_COMMAND_COLOR_WHEN_ASSIGNED = new Color(24, 80, 11); // green

	public static Color OTHER_COMMAND_COLOR_WHEN_UNASSIGNED = new Color(64, 64, 64); // gray

	public static String NO_ASSIGNMENT_LABEL_TEXT = "--Not Set--";

	public static Color NO_ASSIGNMENT_LABEL_COLOR_WHEN_REQUIRED = new Color(138, 33, 33); // red

	public static Color NO_ASSIGNMENT_LABEL_COLOR_WHEN_OPTIONAL = new Color(64, 64, 64); // gray

	public static Color ASSIGNMENT_LABEL_COLOR_FOR_CURRENT_COMMAND = new Color(32, 32, 32); // dark gray

	public static Color ASSIGNMENT_LABEL_COLOR_FOR_FELLOW_COMMANDS = new Color(32, 32, 32); // dark gray

	public JInteractiveBuilderPanel(JInteractiveBuilder builderWidget) {
		super(new BorderLayout(), true);
		this.builderWidget = builderWidget;
		this.commandEntries = new Vector<JCommandEntry>();
		setShowCommandGroupNames(true);
		setShowCommandDescriptions(true);
		setFocusable(true);
		buildUI();
	}

	private void buildUI() {
		JCommandBox box = new JCommandBox();
		for (InputCommand command : getBuilder().getUniqueCommands()) {
			JCommandEntry entry = new JCommandEntry(command);
			getCommandEntries().add(entry);
			box.addEntry(entry);
		}
		add(buildCommandScrollPane(box), BorderLayout.CENTER);
	}

	private JScrollPane buildCommandScrollPane(Box commandBox) {
		JScrollPane scrollPane = new JScrollPane(commandBox, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportBorder(null);
		scrollPane.setBorder(null);
		setCommandScrollPane(scrollPane);
		return scrollPane;
	}

	@Override
	public void notifyChangeOfCurrentCommand(InteractiveBuilder builder) {
		refreshUI();
		final JCommandEntry entry = getCurrentCommandEntry();
		if (entry != null) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					placeCommandInView(entry);
				}
			});
		}
	}

	private void placeCommandInView(JCommandEntry entry) {
		JViewport vport = getCommandScrollPane().getViewport();
		Rectangle vrect = vport.getViewRect();
		Rectangle target = entry.getBounds();
		if (!vrect.contains(target)) {
			int vy = target.y - Math.max((vrect.height - target.height) / 2, 0);
			if (vy < 0) {
				vy = 0;
			} else {
				int vyOverflow = Math.max(vy + vrect.height - vport.getView().getHeight(), 0);
				if (vyOverflow > 0) {
					vy -= vyOverflow;
					if (vy < 0) {
						vy = 0;
					}
				}
			}
			vport.setViewPosition(new Point(0, vy));
		}
	}

	@Override
	public void notifyDeviceAssignment(InteractiveBuilder builder, String deviceIdentifier) {
		refreshUI();
	}

	@Override
	public void notifyCommandAssignment(InteractiveBuilder builder, InputCommand command) {
		// no action
	}

	@Override
	public void notifyChangeOfCommandAssignments(InteractiveBuilder builder) {
		refreshUI();
	}

	@Override
	public void notifySubmitCommandAssignments(InteractiveBuilder builder) {
		if (getConfirmationButton().hasFocus()) {
			getBuilderWidget().doClickConfirmationButton();
		}
	}

	protected void refreshUI() {
		refreshDialogButtons();
		refreshCommandEntries();
		getBuilderWidget().doRefreshDevicesBadge();
	}

	private void refreshDialogButtons() {
		refreshConfirmationButton();
		refreshResetButton();
	}

	private void refreshConfirmationButton() {
		if (getBuilder().isEveryRequiredCommandAssigned()) {
			getConfirmationButton().setEnabled(true);
			if (getBuilder().canSubmitViaControls()) {
				getConfirmationButton().requestFocus();
			} else {
				this.requestFocus();
			}
		} else {
			getConfirmationButton().setEnabled(false);
			this.requestFocus();
		}
	}

	private void refreshResetButton() {
		getResetButton().setEnabled(getBuilder().hasCommandAssignments());
	}

	private void refreshDevicesBadge() {

	}

	private void refreshCommandEntries() {
		Map<GatedInputSelector, String> digitalInputSelectorLabels = new HashMap<>();
		Map<String, Integer> digitalInputSelectorCount = new HashMap<>(); // key = deviceId + inputId
		for (JCommandEntry entry : getCommandEntries()) {
			String assignmentLabelText = NO_ASSIGNMENT_LABEL_TEXT;
			if (entry.isCommandAssigned()) {
				GatedInputSelector inputSelector = getBuilder().getCommandAssignment(entry.getCommand());
				String deviceIdentifier = inputSelector.getDeviceIdentifier();
				String inputIdentifier = inputSelector.getInputIdentifier();
				assignmentLabelText = inputSelector.getInputName();
				boolean digitalInput = inputSelector.getInputFiringRange().isSingleValue();
				if (!digitalInput && hasInputMultipleAssignments(deviceIdentifier, inputIdentifier)) {
					if (inputSelector.getInputFiringRange().getMinimumValue() > 0) {
						assignmentLabelText += "+";
					} else if (inputSelector.getInputFiringRange().getMaximumValue() < 0) {
						assignmentLabelText += "-";
					}
				} else if (digitalInput) {
					if (digitalInputSelectorLabels.containsKey(inputSelector)) {
						assignmentLabelText = digitalInputSelectorLabels.get(inputSelector);
					} else {
						int c = 1;
						String key = deviceIdentifier + "_" + inputIdentifier;
						if (digitalInputSelectorCount.containsKey(key)) {
							c = digitalInputSelectorCount.get(key) + 1;
							assignmentLabelText += "_" + c;
						}
						digitalInputSelectorCount.put(key, c);
						digitalInputSelectorLabels.put(inputSelector, assignmentLabelText);
					}
				}
			}
			entry.refreshUI(assignmentLabelText);
		}
	}

	private boolean hasInputMultipleAssignments(String deviceIdentifier, String inputIdentifier) {
		int found = 0;
		for (InputCommand command : getBuilder().getUniqueCommands()) {
			if (getBuilder().isCommandAssigned(command)) {
				GatedInputSelector inputSelector = getBuilder().getCommandAssignment(command);
				if (inputSelector.getDeviceIdentifier().equals(deviceIdentifier)
						&& inputSelector.getInputIdentifier().equals(inputIdentifier)) {
					if (++found > 1)
						return true;
				}
			}
		}
		return false;
	}

	private JCommandEntry getCurrentCommandEntry() {
		for (JCommandEntry entry : getCommandEntries()) {
			if (entry.isCurrentCommand())
				return entry;
		}
		return null;
	}

	private String getCommandGroupNames(InputCommand command) {
		if (command != null) {
			StringBuilder sb = new StringBuilder(64);
			for (InputCommandGroup group : getBuilder().getCommandGroups()) {
				if (group.hasMember(command)) {
					if (sb.length() > 0)
						sb.append(' ');
					sb.append('<');
					sb.append(group.getGroupName());
					sb.append('>');
				}
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	private ActionableDialogButton getConfirmationButton() {
		return getBuilderWidget().getConfirmationButton();
	}

	private ActionableDialogButton getResetButton() {
		return getBuilderWidget().getResetButton();
	}

	private InteractiveBuilder getBuilder() {
		return getBuilderWidget().getBuilder();
	}

	private JInteractiveBuilder getBuilderWidget() {
		return builderWidget;
	}

	private List<JCommandEntry> getCommandEntries() {
		return commandEntries;
	}

	private JScrollPane getCommandScrollPane() {
		return commandScrollPane;
	}

	private void setCommandScrollPane(JScrollPane scrollPane) {
		this.commandScrollPane = scrollPane;
	}

	public boolean isShowCommandGroupNames() {
		return showCommandGroupNames;
	}

	public void setShowCommandGroupNames(boolean show) {
		this.showCommandGroupNames = show;
	}

	public boolean isShowCommandDescriptions() {
		return showCommandDescriptions;
	}

	public void setShowCommandDescriptions(boolean show) {
		this.showCommandDescriptions = show;
	}

	private class JCommandBox extends Box implements Scrollable {

		public JCommandBox() {
			super(BoxLayout.Y_AXIS);
			setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		}

		public void addEntry(JCommandEntry entry) {
			add(entry);
			add(Box.createVerticalStrut(8));
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 20;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 80;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}

	}

	private class JCommandEntry extends JPanel {

		private InputCommand command;

		private JLabel commandLabel;

		private JLabel assignmentLabel;

		private JLabel groupNamesLabel;

		private JTextArea descriptionField;

		public JCommandEntry(InputCommand command) {
			super(new BorderLayout(4, 0));
			this.command = command;
			this.commandLabel = createCommandLabel();
			this.assignmentLabel = createAssignmentLabel();
			this.groupNamesLabel = createGroupNamesLabel();
			this.descriptionField = createDescriptionField();
			buildUI();
			refreshUI();
		}

		private JLabel createCommandLabel() {
			return new JLabel(getCommand().getName());
		}

		private JLabel createAssignmentLabel() {
			return new JLabel(NO_ASSIGNMENT_LABEL_TEXT);
		}

		private JLabel createGroupNamesLabel() {
			JLabel label = new JLabel("");
			label.setFont(label.getFont().deriveFont(10f));
			return label;
		}

		private JTextArea createDescriptionField() {
			JTextArea field = new JTextArea(1, 20);
			field.setLineWrap(true);
			field.setWrapStyleWord(true);
			field.setEditable(false);
			field.setOpaque(false);
			field.setFont(field.getFont().deriveFont(10f).deriveFont(Font.ITALIC));
			field.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
			return field;
		}

		private void buildUI() {
			add(getCommandLabel(), BorderLayout.WEST);
			add(getAssignmentLabel(), BorderLayout.EAST);
			addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {
					getBuilder().moveToCommand(getCommand());
				}

			});
		}

		private void refreshUI() {
			refreshUI(getAssignmentLabel().getText());
		}

		public void refreshUI(String assignmentLabelText) {
			refreshCommandLabel();
			refreshAssignmentLabel(assignmentLabelText);
			refreshGroupNamesLabel();
			refreshDescriptionField();
			refreshBackground();
			refreshBorder();
		}

		private void refreshCommandLabel() {
			JLabel label = getCommandLabel();
			Font font = label.getFont();
			Color color = Color.BLACK;
			if (isCurrentCommand()) {
				font = font.deriveFont(36f);
				if (isCommandAssigned()) {
					color = CURRENT_COMMAND_COLOR_WHEN_ASSIGNED;
				} else {
					color = CURRENT_COMMAND_COLOR_WHEN_UNASSIGNED;
				}
			} else {
				font = font.deriveFont(14f);
				if (isCommandAssigned()) {
					color = OTHER_COMMAND_COLOR_WHEN_ASSIGNED;
				} else {
					color = OTHER_COMMAND_COLOR_WHEN_UNASSIGNED;
				}
			}
			label.setFont(font);
			label.setForeground(color);
		}

		private void refreshAssignmentLabel(String assignmentLabelText) {
			JLabel label = getAssignmentLabel();
			Font font = label.getFont();
			if (isCurrentCommand()) {
				font = font.deriveFont(18f);
			} else {
				font = font.deriveFont(14f);
			}
			Color color = Color.BLACK;
			if (isCommandAssigned()) {
				font = font.deriveFont(Font.BOLD);
				if (isCurrentCommand()) {
					color = ASSIGNMENT_LABEL_COLOR_FOR_CURRENT_COMMAND;
				} else if (isFellowGroupCommand()) {
					color = ASSIGNMENT_LABEL_COLOR_FOR_FELLOW_COMMANDS;
				} else {
					color = getBackground().darker();
				}
			} else {
				font = font.deriveFont(Font.ITALIC);
				if (isCommandRequired()) {
					color = NO_ASSIGNMENT_LABEL_COLOR_WHEN_REQUIRED;
				} else {
					color = NO_ASSIGNMENT_LABEL_COLOR_WHEN_OPTIONAL;
				}
			}
			label.setText(assignmentLabelText);
			label.setFont(font);
			label.setForeground(color);
		}

		private void refreshGroupNamesLabel() {
			JLabel label = getGroupNamesLabel();
			if (isShowCommandGroupNames() && isCurrentCommand()) {
				label.setText(getCommandGroupNames(getCommand()));
				label.setForeground(getCommandLabel().getForeground());
				add(label, BorderLayout.NORTH);
			} else {
				remove(label);
			}
		}

		private void refreshDescriptionField() {
			JTextArea field = getDescriptionField();
			if (isShowCommandDescriptions() && isCurrentCommand() && getCommand().hasDescription()) {
				field.setText(getCommand().getDescription());
				field.setForeground(getCommandLabel().getForeground());
				add(field, BorderLayout.SOUTH);
			} else {
				remove(field);
			}
		}

		private void refreshBackground() {
			Color bg = null;
			if (isCurrentCommand()) {
				bg = CURRENT_COMMAND_BACKGROUND;
			}
			setBackground(bg);
		}

		private void refreshBorder() {
			Border border = null;
			if (isCurrentCommand()) {
				border = BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(CURRENT_COMMAND_BORDER_COLOR),
						BorderFactory.createEmptyBorder(2, 4, 2, 4));
			}
			setBorder(border);
		}

		public boolean isCurrentCommand() {
			return getCommand().equals(getBuilder().getCurrentCommand());
		}

		public boolean isCommandAssigned() {
			return getBuilder().isCommandAssigned(getCommand());
		}

		public boolean isCommandRequired() {
			return getBuilder().isCommandRequired(getCommand());
		}

		public boolean isFellowGroupCommand() {
			if (getBuilder().getCurrentCommand() != null) {
				return getBuilder().getFellowGroupMembers(getBuilder().getCurrentCommand()).contains(getCommand());
			} else {
				return false;
			}
		}

		public InputCommand getCommand() {
			return command;
		}

		private JLabel getCommandLabel() {
			return commandLabel;
		}

		private JLabel getAssignmentLabel() {
			return assignmentLabel;
		}

		private JLabel getGroupNamesLabel() {
			return groupNamesLabel;
		}

		private JTextArea getDescriptionField() {
			return descriptionField;
		}

	}

}