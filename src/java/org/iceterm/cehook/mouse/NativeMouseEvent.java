package org.iceterm.cehook.mouse;

import org.iceterm.cehook.GlobalScreen;
import org.iceterm.cehook.NativeInputEvent;

import java.awt.*;

/**
 * An event which indicates that a mouse action occurred on the system.
 * This event is not limited to a particular component's scope or visibility.
 * <p>
 *
 * This low-level event is generated natively for:
 * <ul>
 * 	<li>
 * 		Native Mouse Events
 * 		<ul>
 * 			<li>a mouse button is pressed</li>
 * 			<li>a mouse button is released</li>
 * 			<li>the mouse wheel was moved</li>
 * 		</ul>
 * 	</li>
 * 	<li>
 * 		Native Mouse Motion Events
 * 		<ul>
 * 			<li>the mouse is moved
 * 			<li>the mouse is dragged</li>
 * 		</ul>
 * 	</li>
 * </ul>
 * <p>
 *
 * A <code>NativeMouseEvent</code> object is passed to every
 * <code>NativeMouseListener</code> object which is registered to receive mouse
 * events using the {@link GlobalScreen#addNativeMouseListener} method. The
 * <code>GlobalScreen</code> object then dispatches a
 * <code>NativeMouseEvent</code> to each listener.
 * <p>
 *
 * A <code>NativeMouseEvent</code> object is also passed to every
 * <code>NativeMouseMotionListener</code> object which has been registered to
 * receive mouse motion events using the
 * {@link GlobalScreen#addNativeMouseListener} method. The
 * <code>GlobalScreen</code> object then dispatches a
 * <code>NativeMouseEvent</code> to each listener.
 * <p>
 *
 * When a mouse button is clicked, events are generated and sent to the
 * registered <code>NativeMouseListeners</code>. The state of modifiers can be
 * retrieved using {@link NativeInputEvent#getModifiers}. All modifiers returned
 * by {@link NativeInputEvent#getModifiers} reflect the current state of all
 * modifiers and buttons.
 *
 * @see GlobalScreen
 * @see NativeMouseListener
 */
public class NativeMouseEvent extends NativeInputEvent {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1093048990695350863L;

	/**
	 * The native mouse event's x pointer position.
	 *
	 * @see #getX()
	 */
	private int x;

	/**
	 * The native mouse event's y pointer position.
	 *
	 * @see #getY()
	 */
	private int y;

	/**
	 * The native mouse event's click count.
	 *
	 * @see #getClickCount()
	 */
	private int clickCount;

	/**
	 * Indicates which, if any, of the mouse buttons has changed state.
	 *
	 * The only legal values are the following constants:
	 * <code>NOBUTTON</code>,
	 * <code>BUTTON1</code>,
	 * <code>BUTTON2</code>,
	 * <code>BUTTON3</code>,
	 * <code>BUTTON4</code> or
	 * <code>BUTTON5</code>
	 */
	private int button;

	/** The first number in the range of id's used for native mouse events. */
	public static final int NATIVE_MOUSE_FIRST		= 2500;

	/** The last number in the range of id's used for native mouse events. */
	public static final int NATIVE_MOUSE_LAST		= 2505;

	/**
	 * The native mouse pressed event. This <code>NativeMouseEvent</code>
	 * occurs when a mouse button is pressed and released.
	 */
	public static final int NATIVE_MOUSE_CLICKED	= NATIVE_MOUSE_FIRST;

	/**
	 * The native mouse pressed event. This <code>NativeMouseEvent</code>
	 * occurs when a mouse button is depressed.
	 */
	public static final int NATIVE_MOUSE_PRESSED	= 1 + NATIVE_MOUSE_FIRST;

	/**
	 * The native mouse released event. This <code>NativeMouseEvent</code>
	 * occurs when a mouse button is released.
	 */
	public static final int NATIVE_MOUSE_RELEASED	= 2 + NATIVE_MOUSE_FIRST;

	/**
	 * The native mouse moved event. This <code>NativeMouseEvent</code>
	 * occurs when the mouse pointer changes position.
	 */
	public static final int NATIVE_MOUSE_MOVED		= 3 + NATIVE_MOUSE_FIRST;

	/**
	 * The native mouse dragged event. This <code>NativeMouseEvent</code>
	 * occurs when the mouse pointer changes position while a mouse button
	 * is pressed.
	 */
	public static final int NATIVE_MOUSE_DRAGGED	= 4 + NATIVE_MOUSE_FIRST;

	/**
	 * The native mouse dragged event. This <code>NativeMouseEvent</code>
	 * occurs when the mouse wheel is rotated.
	 */
	public static final int NATIVE_MOUSE_WHEEL		= 5 + NATIVE_MOUSE_FIRST;


	/** Indicates no mouse buttons; used by getButton(). */
	public static final int	NOBUTTON				= 0;

	/** Indicates mouse button #1; used by getButton(). */
	public static final int	BUTTON1					= 1;

	/** Indicates mouse button #2; used by getButton(). */
	public static final int	BUTTON2					= 2;

	/** Indicates mouse button #3; used by getButton(). */
	public static final int	BUTTON3					= 3;

	/** Indicates mouse button #4; used by getButton(). */
	public static final int	BUTTON4					= 4;

	/** Indicates mouse button #5; used by getButton(). */
	public static final int	BUTTON5					= 5;


	/**
	 * Instantiates a new <code>NativeMouseEvent</code> object.
	 *
	 * @param id an integer that identifies the native event type.
	 * @param modifiers a modifier mask describing the modifier keys and mouse
	 * buttons active for the event.
	 * <code>NativeInputEvent _MASK</code> modifiers should be used as they are
	 * not compatible with the extended _DOWN_MASK or the old _MASK
	 * <code>InputEvent</code> modifiers.
	 * @param x the x coordinate of the native pointer.
	 * @param y the y coordinate of the native pointer.
	 * @param clickCount the number of button clicks associated with this event.
	 *
	 * @since 1.1
	 */
	public NativeMouseEvent(int id, int modifiers, int x, int y, int clickCount) {
		this(id, modifiers, x, y, clickCount, NOBUTTON);
	}

	/**
	 * Instantiates a new <code>NativeMouseEvent</code> object.
	 *
	 * @param id an integer that identifies the native event type.
	 * @param modifiers a modifier mask describing the modifier keys and mouse
	 * buttons active for the event.
	 * <code>NativeInputEvent</code> _MASK modifiers should be used as they are
	 * not compatible with the extended _DOWN_MASK or the old _MASK
	 * <code>InputEvent</code> modifiers.
	 * @param x the x coordinate of the native pointer.
	 * @param y the y coordinate of the native pointer.
	 * @param clickCount the number of button clicks associated with this event.
	 * @param button The mouse buttons that have changed state. NOBUTTON,
	 * BUTTON1, BUTTON2, BUTTON3, BUTTON5 or BUTTON5.
	 *
	 * @since 1.1
	 */
	public NativeMouseEvent(int id, int modifiers, int x, int y, int clickCount, int button) {
		super(GlobalScreen.class, id, modifiers);

		this.x = x;
		this.y = y;
		this.clickCount = clickCount;
		this.button = button;
	}

	/**
	 * Returns which, if any, of the mouse buttons has changed state.
	 *
	 * @return one of the following constants:
	 * <code>NOBUTTON</code>,
	 * <code>BUTTON1</code>,
	 * <code>BUTTON2</code>,
	 * <code>BUTTON3</code>,
	 * <code>BUTTON4</code> or
	 * <code>BUTTON5</code>
	 */
	public int getButton() {
		return button;
	}

	/**
	 * Returns the number of button clicks associated with this event.
	 *
	 * @return An integer indicating the number of button clicks
	 *
	 * @since 1.1
	 */
	public int getClickCount() {
		return clickCount;
	}

	/**
	 * Returns the x,y position of the native event.
	 *
	 * @return a <code>Point</code> object containing the x and y coordinates
	 * of the native pointer
	 *
	 * @since 1.1
	 */
	public Point getPoint() {
		return new Point(x, y);
	}

	/**
	 * Returns the x coordinate of the native event.
	 *
	 * @return x an integer indicating horizontal position of the native pointer
	 */
	public int getX() {
		return x;
	}

	/**
	 * Returns the y coordinate of the native event.
	 *
	 * @return y an integer indicating horizontal position of the native pointer
	 */
	public int getY() {
		return y;
	}
}
