package org.maia.io.inputdevice.tuner;

import org.maia.io.inputdevice.InputEvent;
import org.maia.io.inputdevice.InputValueRange;
import org.maia.util.GenericListener;

public interface InputTunerListener extends GenericListener {

	void notifyTunerWaitingForSilence(InputTuner tuner);

	void notifyTunerSilenceReached(InputTuner tuner);

	void notifyTunerEvent(InputTuner tuner, InputEvent event, InputValueRange tuningRange);

}