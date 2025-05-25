package org.maia.io.inputdevice;

public interface InputFilter {

	InputFilter ACCEPT_ALL = new InputFilter() {

		@Override
		public boolean accept(Input input) {
			return true;
		}
	};

	boolean accept(Input input);

}