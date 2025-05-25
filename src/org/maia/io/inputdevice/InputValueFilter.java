package org.maia.io.inputdevice;

public interface InputValueFilter {

	InputValueFilter ACCEPT_ALL = new InputValueFilter() {

		@Override
		public boolean accept(float value) {
			return true;
		}
	};

	boolean accept(float value);

}