package org.maia.io.inputdevice.controller;

import org.maia.io.inputdevice.DigitalInputValueRange;
import org.maia.io.inputdevice.InputEvent;
import org.maia.io.inputdevice.InputValueRange;

public class GatedInputCommandProducer extends InputCommandProducer {

	private InputValueRange inputFiringRange;

	private boolean firing;

	private float fastReleasingPivotValue; // cast on positive axis

	private float fastReleasingDeltaValue = 0.02f; // cast on positive axis

	public GatedInputCommandProducer(InputCommand command) {
		this(command, GatedInputController.DEFAULT_INPUT_FIRING_VALUE);
	}

	public GatedInputCommandProducer(InputCommand command, float inputFiringValue) {
		this(command, new DigitalInputValueRange(inputFiringValue));
	}

	public GatedInputCommandProducer(InputCommand command, InputValueRange inputFiringRange) {
		super(command);
		this.inputFiringRange = inputFiringRange;
	}

	@Override
	public synchronized void init(InputController controller) {
		super.init(controller);
		setFiring(false);
		setFastReleasingPivotValue(getInitialFastReleasingPivotValue());
	}

	@Override
	public synchronized void process(InputEvent event, InputController controller) {
		if (controller instanceof GatedInputController) {
			GatedInputController gatedController = (GatedInputController) controller;
			if (event.getInput().isDigital() || !gatedController.isFastReleasing()) {
				processStandardInput(event, gatedController);
			} else {
				processAnalogFastReleasingInput(event, gatedController);
			}
		}
	}

	private void processStandardInput(InputEvent event, GatedInputController gatedController) {
		boolean wasFiring = isFiring();
		boolean firing = getInputFiringRange().accept(event.getValue());
		setFiring(firing);
		if (wasFiring ^ firing) {
			if (firing) {
				gatedController.fireInputCommand(getCommand());
			} else {
				gatedController.releaseInputCommand(getCommand());
			}
		}
	}

	private void processAnalogFastReleasingInput(InputEvent event, GatedInputController gatedController) {
		float value = isPositiveAxis() ? event.getValue() : -event.getValue();
		float pivot = getFastReleasingPivotValue();
		if (!isFiring()) {
			float iniPivot = getInitialFastReleasingPivotValue();
			if (value >= pivot) {
				// fire
				setFiring(true);
				setFastReleasingPivotValue(Math.max(value - getFastReleasingDeltaValue(), iniPivot));
				gatedController.fireInputCommand(getCommand());
			} else {
				setFastReleasingPivotValue(Math
						.max(Math.min(getFastReleasingPivotValue(), value + getFastReleasingDeltaValue()), iniPivot));
			}
		} else {
			if (value < pivot) {
				// release fire
				setFiring(false);
				setFastReleasingPivotValue(
						Math.min(value + getFastReleasingDeltaValue(), getMaximumFastReleasingPivotValue()));
				gatedController.releaseInputCommand(getCommand());
			} else {
				// firing and rising
				setFastReleasingPivotValue(
						Math.max(value - getFastReleasingDeltaValue(), getFastReleasingPivotValue()));
			}
		}
	}

	@Override
	public synchronized void exit(InputController controller) {
		super.exit(controller);
		setFastReleasingPivotValue(getInitialFastReleasingPivotValue());
		if (isFiring()) {
			setFiring(false);
			if (controller instanceof GatedInputController) {
				((GatedInputController) controller).releaseInputCommand(getCommand());
			}
		}
	}

	private boolean isPositiveAxis() {
		return getInputFiringRange().getMinimumValue() >= 0;
	}

	private float getInitialFastReleasingPivotValue() {
		return isPositiveAxis() ? getInputFiringRange().getMinimumValue() : -getInputFiringRange().getMaximumValue();
	}

	private float getMaximumFastReleasingPivotValue() {
		return isPositiveAxis() ? getInputFiringRange().getMaximumValue() : -getInputFiringRange().getMinimumValue();
	}

	private InputValueRange getInputFiringRange() {
		return inputFiringRange;
	}

	protected boolean isFiring() {
		return firing;
	}

	private void setFiring(boolean firing) {
		this.firing = firing;
	}

	private float getFastReleasingPivotValue() {
		return fastReleasingPivotValue;
	}

	private void setFastReleasingPivotValue(float pivotValue) {
		this.fastReleasingPivotValue = pivotValue;
	}

	public float getFastReleasingDeltaValue() {
		return fastReleasingDeltaValue;
	}

	public void setFastReleasingDeltaValue(float deltaValue) {
		this.fastReleasingDeltaValue = deltaValue;
	}

}