package org.maia.io.inputdevice.controller.config.ia;

import java.io.File;
import java.util.List;
import java.util.Vector;

public class BluetoothSettingsCalloutExecutable implements BluetoothSettingsCallout {

	private List<Executable> executables;

	public BluetoothSettingsCalloutExecutable() {
		this.executables = new Vector<Executable>();
		loadCommonExecutables();
	}

	protected void loadCommonExecutables() {
		addExecutable(new Executable(new File("/usr/bin/blueman-manager"))); // Linux
		addExecutable(new Executable(new File("C:\\Windows\\explorer.exe"), "ms-settings:bluetooth")); // Windows 10-11
	}

	public void addExecutable(Executable executable) {
		getExecutables().add(executable);
	}

	@Override
	public boolean canCallout() {
		return chooseExecutableToCallout() != null;
	}

	@Override
	public void callout() {
		Executable executable = chooseExecutableToCallout();
		if (executable != null) {
			executable.execute();
		}
	}

	protected Executable chooseExecutableToCallout() {
		Executable chosen = null;
		for (Executable executable : getExecutables()) {
			if (executable.canExecute()) {
				chosen = executable;
				break;
			}
		}
		return chosen;
	}

	private List<Executable> getExecutables() {
		return executables;
	}

	public static class Executable {

		private File file;

		private String arguments;

		public Executable(File file) {
			this(file, null);
		}

		public Executable(File file, String arguments) {
			if (file == null)
				throw new NullPointerException("Must specify an executable file");
			this.file = file;
			this.arguments = arguments;
		}

		public boolean canExecute() {
			return getFile().canExecute();
		}

		public void execute() {
			try {
				String commandLine = getCommandLine();
				System.out.println("Executing " + commandLine);
				Runtime.getRuntime().exec(commandLine.split(" "));
			} catch (Exception e) {
				System.err.println("Failed to execute");
				System.err.println(e);
			}
		}

		private String getCommandLine() {
			String commandLine = getFile().getAbsolutePath();
			if (hasArguments()) {
				commandLine += " " + getArguments().trim();
			}
			return commandLine;
		}

		public boolean hasArguments() {
			return getArguments() != null && !getArguments().isEmpty();
		}

		public File getFile() {
			return file;
		}

		public String getArguments() {
			return arguments;
		}

	}

}