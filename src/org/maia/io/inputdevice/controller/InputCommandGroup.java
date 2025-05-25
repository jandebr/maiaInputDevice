package org.maia.io.inputdevice.controller;

import java.util.List;
import java.util.Vector;

public class InputCommandGroup {

	private String groupName;

	private List<InputCommand> members;

	public InputCommandGroup(String groupName) {
		if (groupName == null || groupName.isEmpty())
			throw new IllegalArgumentException("Must specify a non-empty group name");
		this.groupName = groupName;
		this.members = new Vector<InputCommand>();
	}

	public void addMember(InputCommand command) {
		if (!hasMember(command)) {
			getMembers().add(command);
		}
	}

	public boolean hasMember(InputCommand command) {
		return getMembers().contains(command);
	}

	public String getGroupName() {
		return groupName;
	}

	public List<InputCommand> getMembers() {
		return members;
	}

}