package org.maia.io.inputdevice.impl.jinput;

import java.util.List;
import java.util.Vector;

import org.maia.io.inputdevice.Input;
import org.maia.io.inputdevice.InputFilter;

import net.java.games.input.Component.Identifier.Axis;

public class AxisInputFilter implements InputFilter {

	private FilterMode mode;

	private List<Axis> axis;

	public static Axis SLIDER_AXIS = Axis.SLIDER;

	public static Axis[] ROTATIONAL_AXIS = new Axis[] { Axis.RX, Axis.RY, Axis.RZ };

	public static Axis[] FORCE_AXIS = new Axis[] { Axis.SLIDER_FORCE, Axis.X_FORCE, Axis.Y_FORCE, Axis.Z_FORCE,
			Axis.RX_FORCE, Axis.RY_FORCE, Axis.RZ_FORCE };

	public static Axis[] VELOCITY_AXIS = new Axis[] { Axis.SLIDER_VELOCITY, Axis.X_VELOCITY, Axis.Y_VELOCITY,
			Axis.Z_VELOCITY, Axis.RX_VELOCITY, Axis.RY_VELOCITY, Axis.RZ_VELOCITY };

	public static Axis[] ACCELERATION_AXIS = new Axis[] { Axis.SLIDER_ACCELERATION, Axis.X_ACCELERATION,
			Axis.Y_ACCELERATION, Axis.Z_ACCELERATION, Axis.RX_ACCELERATION, Axis.RY_ACCELERATION,
			Axis.RZ_ACCELERATION };

	public AxisInputFilter(FilterMode mode) {
		this.mode = mode;
		this.axis = new Vector<Axis>();
	}

	public static AxisInputFilter createExplicitUserGestureFilter() {
		return createExclusiveFilter(AxisInputFilter.SLIDER_AXIS).addAxis(AxisInputFilter.ROTATIONAL_AXIS)
				.addAxis(AxisInputFilter.FORCE_AXIS).addAxis(AxisInputFilter.VELOCITY_AXIS)
				.addAxis(AxisInputFilter.ACCELERATION_AXIS);
	}

	public static AxisInputFilter createInclusiveFilter(Axis... axisToInclude) {
		AxisInputFilter filter = new AxisInputFilter(FilterMode.INCLUSIVE);
		filter.addAxis(axisToInclude);
		return filter;
	}

	public static AxisInputFilter createExclusiveFilter(Axis... axisToExclude) {
		AxisInputFilter filter = new AxisInputFilter(FilterMode.EXCLUSIVE);
		filter.addAxis(axisToExclude);
		return filter;
	}

	public AxisInputFilter addAxis(Axis axis) {
		getAxis().add(axis);
		return this;
	}

	public AxisInputFilter addAxis(Axis... axis) {
		for (Axis a : axis)
			addAxis(a);
		return this;
	}

	@Override
	public boolean accept(Input input) {
		String axisName = input.getIdentifier();
		if (isInclusive()) {
			return hasAxisByName(axisName);
		} else {
			return !hasAxisByName(axisName);
		}
	}

	private boolean hasAxisByName(String axisName) {
		for (Axis axis : getAxis()) {
			if (axis.getName().equals(axisName))
				return true;
		}
		return false;
	}

	public boolean isInclusive() {
		return FilterMode.INCLUSIVE.equals(getMode());
	}

	public boolean isExclusive() {
		return FilterMode.EXCLUSIVE.equals(getMode());
	}

	public FilterMode getMode() {
		return mode;
	}

	private List<Axis> getAxis() {
		return axis;
	}

	public static enum FilterMode {

		INCLUSIVE,

		EXCLUSIVE;

	}

}