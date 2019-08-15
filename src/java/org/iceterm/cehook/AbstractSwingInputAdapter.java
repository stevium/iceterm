package org.iceterm.cehook;

import org.iceterm.cehook.keyboard.NativeKeyEvent;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Adapter to convert convert java modifiers to native
 */
public abstract class AbstractSwingInputAdapter extends Component {

	protected int getJavaModifiers(int nativeModifiers) {
		int modifiers = 0x00;
		if ((nativeModifiers & NativeInputEvent.SHIFT_MASK) != 0) {
			modifiers |= KeyEvent.SHIFT_MASK;
			modifiers |= KeyEvent.SHIFT_DOWN_MASK;
		}
		if ((nativeModifiers & NativeInputEvent.META_MASK) != 0) {
			modifiers |= KeyEvent.META_MASK;
			modifiers |= KeyEvent.META_DOWN_MASK;
		}
		if ((nativeModifiers & NativeInputEvent.META_MASK) != 0) {
			modifiers |= KeyEvent.CTRL_MASK;
			modifiers |= KeyEvent.CTRL_DOWN_MASK;
		}
		if ((nativeModifiers & NativeInputEvent.ALT_MASK) != 0) {
			modifiers |= KeyEvent.ALT_MASK;
			modifiers |= KeyEvent.ALT_DOWN_MASK;
		}
		if ((nativeModifiers & NativeInputEvent.BUTTON1_MASK) != 0) {
			modifiers |= KeyEvent.BUTTON1_MASK;
			modifiers |= KeyEvent.BUTTON1_DOWN_MASK;
		}
		if ((nativeModifiers & NativeInputEvent.BUTTON2_MASK) != 0) {
			modifiers |= KeyEvent.BUTTON2_MASK;
			modifiers |= KeyEvent.BUTTON2_DOWN_MASK;
		}
		if ((nativeModifiers & NativeInputEvent.BUTTON3_MASK) != 0) {
			modifiers |= KeyEvent.BUTTON3_MASK;
			modifiers |= KeyEvent.BUTTON3_DOWN_MASK;
		}

		return modifiers;
	}

	public static int getNativeModifiers(int modifiers) {
		int newModifiers = 0;
		if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
			newModifiers |= NativeKeyEvent.SHIFT_MASK;
		}
		if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
			newModifiers |= NativeKeyEvent.ALT_MASK;
		}
		if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
			newModifiers |= NativeKeyEvent.ALT_MASK;
		}
		if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
			newModifiers |= NativeKeyEvent.CTRL_MASK;
		}
		if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
			newModifiers |= NativeKeyEvent.META_MASK;
		}

		return newModifiers;
	}
}
