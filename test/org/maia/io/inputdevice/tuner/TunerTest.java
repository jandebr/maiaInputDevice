package org.maia.io.inputdevice.tuner;

import org.maia.io.inputdevice.InputDeviceFilter;
import org.maia.io.inputdevice.InputEvent;
import org.maia.io.inputdevice.InputValueRange;
import org.maia.io.inputdevice.Test;

public class TunerTest extends Test implements InputTunerListener {

	public static void main(String[] args) {
		new TunerTest().startTest();
	}

	@Override
	public void startTest() {
		showFrame("Tuner test");
		InputTuner tuner = new InputTuner(InputDeviceFilter.STICK_OR_GAMEPAD);
		tuner.addListener(this);
		tuner.startTuning(true);
	}

	@Override
	public void notifyTunerWaitingForSilence(InputTuner tuner) {
		System.out.println("Waiting for silence");
	}

	@Override
	public void notifyTunerSilenceReached(InputTuner tuner) {
		System.out.println("Silence reached");
	}

	@Override
	public void notifyTunerEvent(InputTuner tuner, InputEvent event, InputValueRange tuningRange) {
		System.out.println("Device '" + event.getDevice().getIdentifier() + "' input '" + event.getInput().getName()
				+ "' = " + event.getValue() + " in range " + tuningRange);
		tuner.waitForSilence();
	}

}