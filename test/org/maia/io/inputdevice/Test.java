package org.maia.io.inputdevice;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public abstract class Test {

	protected Test() {
	}

	public abstract void startTest() throws Exception;

	protected JFrame showFrame(String title) {
		return showFrame(title, Box.createRigidArea(new Dimension(320, 240)));
	}

	protected JFrame showFrame(String title, Component component) {
		JFrame frame = new JFrame(title);
		frame.add(component);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		return frame;
	}

}