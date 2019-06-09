package org.iceterm.cehook;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Adapter to convert convert native modifiers to java modifiers
 * and to provide an anchor point for native evnets.
 * The methods are empty so the super call is obsolete.
 *
 * @since 2.1
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
}
