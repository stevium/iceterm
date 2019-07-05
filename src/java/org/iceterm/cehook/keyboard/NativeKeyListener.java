package org.iceterm.cehook.keyboard;

// Imports.

import org.iceterm.cehook.GlobalScreen;
import java.util.EventListener;

/**
 * The listener interface for receiving global <code>NativeKeyEvents</code>.
 * <p>
 *
 * The class that is interested in processing a <code>NativeKeyEvent</code>
 * implements this interface, and the object created with that class is
 * registered with the <code>GlobalScreen</code> using the
 * {@link GlobalScreen#addNativeKeyListener(NativeKeyListener)} method. When the
 * <code>NativeKeyEvent</code> occurs, that object's appropriate method is
 * invoked.
 *
 * @see NativeKeyEvent
 */
public interface NativeKeyListener extends EventListener {

	/**
	 * Invoked when a key has been typed.
	 *
	 * @param nativeEvent the native key event.
	 *
	 * @since 1.1
	 */
	public void nativeKeyTyped(NativeKeyEvent nativeEvent);

	/**
	 * Invoked when a key has been pressed.
	 *
	 * @param nativeEvent the native key event.
	 */
	public void nativeKeyPressed(NativeKeyEvent nativeEvent);

	/**
	 * Invoked when a key has been released.
	 *
	 * @param nativeEvent the native key event.
	 */
	public void nativeKeyReleased(NativeKeyEvent nativeEvent);
}
