package org.iceterm.cehook.mouse;

import org.iceterm.cehook.GlobalScreen;

import java.util.EventListener;

/**
 * The listener interface for receiving systemwide <code>NativeMouseEvents</code>.
 * (To track native mouse moves, use the <code>NativeMouseMotionListener</code>.)
 * <p>
 *
 * The class that is interested in processing a <code>NativeMouseEvent</code>
 * implements this interface, and the object created with that class is
 * registered with the <code>GlobalScreen</code> using the
 * {@link GlobalScreen#addNativeMouseListener} method. When the
 * <code>NativeMouseMotion</code> event occurs, that object's appropriate
 * method is invoked.
 *
 * @see NativeMouseEvent
 */
public interface NativeMouseListener extends EventListener {
	/**
	 * Invoked when a mouse button has been clicked (pressed and released)
	 * without being moved.
	 *
	 * @param nativeEvent the native mouse event.
	 */
	public void nativeMouseClicked(NativeMouseEvent nativeEvent);

	/**
	 * Invoked when a mouse button has been pressed
	 *
	 * @param nativeEvent the native mouse event.
	 */
	public void nativeMousePressed(NativeMouseEvent nativeEvent);

	/**
	 * Invoked when a mouse button has been released
	 *
	 * @param nativeEvent the native mouse event.
	 */
	public void nativeMouseReleased(NativeMouseEvent nativeEvent);
}

