package org.maia.io.inputdevice;

public class InputEvent {

	private Input input;

	private float value;

	private long creationTime;

	public InputEvent(Input input, float value) {
		this.input = input;
		this.value = value;
		this.creationTime = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return getInput().toString() + " = " + getValue();
	}

	public InputDevice getDevice() {
		return getInput().getDevice();
	}

	public Input getInput() {
		return input;
	}

	protected void setInput(Input input) {
		this.input = input;
	}

	public float getValue() {
		return value;
	}

	protected void setValue(float value) {
		this.value = value;
	}

	public long getCreationTime() {
		return creationTime;
	}

	protected void setCreationTime(long time) {
		this.creationTime = time;
	}
	
}