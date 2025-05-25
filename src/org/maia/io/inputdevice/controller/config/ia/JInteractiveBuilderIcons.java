package org.maia.io.inputdevice.controller.config.ia;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class JInteractiveBuilderIcons {

	public static ImageIcon refreshIcon = loadIcon("refresh32.png");

	public static ImageIcon bluetoothIcon = loadIcon("bluetooth32.png");

	public static ImageIcon controllerIcon = loadIcon("controller32.png");

	public static ImageIcon typeKeyboardIcon = loadIcon("type-keyboard32.png");

	public static ImageIcon typeKeyboardDisabledIcon = loadIcon("type-keyboard-faded32.png");

	public static ImageIcon typeMouseIcon = loadIcon("type-mouse32.png");

	public static ImageIcon typeMouseDisabledIcon = loadIcon("type-mouse-faded32.png");

	public static ImageIcon typeGamepadIcon = loadIcon("type-gamepad32.png");

	public static ImageIcon typeGamepadDisabledIcon = loadIcon("type-gamepad-faded32.png");

	public static ImageIcon typeStickIcon = loadIcon("type-stick32.png");

	public static ImageIcon typeStickDisabledIcon = loadIcon("type-stick-faded32.png");

	public static ImageIcon typeOtherIcon = loadIcon("type-other32.png");

	public static ImageIcon typeOtherDisabledIcon = loadIcon("type-other-faded32.png");

	public static ImageIcon typeUnknownIcon = loadIcon("type-unknown32.png");

	public static ImageIcon typeUnknownDisabledIcon = loadIcon("type-unknown-faded32.png");

	private static ImageIcon loadIcon(String resourceName) {
		ImageIcon icon = null;
		try {
			InputStream in = JInteractiveBuilderIcons.class.getResourceAsStream("icons/" + resourceName);
			icon = new ImageIcon(ImageIO.read(in));
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return icon;
	}

}