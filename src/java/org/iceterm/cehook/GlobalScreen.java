package org.iceterm.cehook;

import org.iceterm.cehook.dispatcher.DefaultDispatchService;
import org.iceterm.cehook.keyboard.NativeKeyEvent;
import org.iceterm.cehook.keyboard.NativeKeyListener;
import org.iceterm.cehook.mouse.NativeMouseEvent;
import org.iceterm.cehook.mouse.NativeMouseListener;

import javax.swing.event.EventListenerList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * GlobalScreen is used to represent the native screen area that Java does not
 * usually have access to. This class can be thought of as the source component
 * for native input events.
 * <p>
 * This class also handles the loading, unpacking and communication with the
 * native library. That includes registering and un-registering the native hook
 * with the underlying operating system and adding global keyboard and mouse
 * listeners.
 */
public class GlobalScreen {
	/**
	 * Logging service for the native library.
	 */
	protected static Logger log = Logger.getLogger(GlobalScreen.class.getPackage().getName());

	/**
	 * The service to control the hook.
	 */
	protected static NativeHookThread hookThread;

	/**
	 * The service to dispatch events.
	 */
	protected static ExecutorService eventExecutor;

	/**
	 * The list of event listeners to notify.
	 */
	protected static EventListenerList eventListeners = new EventListenerList();

	protected GlobalScreen() { }

	/**
	 * Adds the specified native key listener to receive key events from the
	 * native system. If listener is null, no exception is thrown and no action
	 * is performed.
	 *
	 * @param listener a native key listener object
	 */
	public static void addNativeKeyListener(NativeKeyListener listener) {
		if (listener != null) {
			eventListeners.add(NativeKeyListener.class, listener);
		}
	}

	/**
	 * Removes the specified native key listener so that it no longer receives
	 * key events from the native system. This method performs no function if
	 * the listener specified by the argument was not previously added.  If
	 * listener is null, no exception is thrown and no action is performed.
	 *
	 * @param listener a native key listener object
	 */
	public static void removeNativeKeyListener(NativeKeyListener listener) {
		if (listener != null) {
			eventListeners.remove(NativeKeyListener.class, listener);
		}
	}

	/**
	 * Adds the specified native mouse listener to receive mouse events from the
	 * native system. If listener is null, no exception is thrown and no action
	 * is performed.
	 *
	 * @param listener a native mouse listener object
	 */
	public static void addNativeMouseListener(NativeMouseListener listener) {
		if (listener != null) {
			eventListeners.add(NativeMouseListener.class, listener);
		}
	}


	/**
	 * Specialized thread implementation for the native hook.
	 */
	public static class NativeHookThread extends Thread {
		/**
		 * Exception thrown by this thread.
		 */
		protected NativeHookException exception;

		/**
		 * Default constructor.
		 */
		public NativeHookThread() {
			this.setName("JNativeHook Hook Thread");
			this.setDaemon(false);
			this.setPriority(Thread.MAX_PRIORITY);
		}

		public void run() {
			this.exception = null;

			try {
				// NOTE enable() will call notifyAll() on this object after passing exception throwing code.
				this.enable();
			}
			catch (NativeHookException e) {
				this.exception = e;
			}

			synchronized (this) {
				// Notify anyone that is still waiting for the hook that it has completed.
				this.notifyAll();
			}
		}

		/**
		 * Get the exception associated with the current hook, or null of no exception was thrown.
		 *
		 * @return the <code>NativeHookException</code> or null.
		 */
		public NativeHookException getException() {
			return this.exception;
		}

		/**
		 * Native implementation to start the input hook.  This method blocks and should only be called by this
		 * specialized thread implementation.  This method will notifyAll() after passing any exception exception
		 * throwing code.
		 *
		 * @throws NativeHookException problem registering the native hook with the underlying operating system.
		 */
		protected native void enable() throws NativeHookException;

		/**
		 * Native implementation to stop the input hook.  There is no other way to stop the hook.
		 *
		 * @throws NativeHookException problem un-registering the native hook with the underlying operating system.
		 */
		public native void disable() throws NativeHookException;

		/**
		 * Dispatches an event to the appropriate processor.  This method is
		 * generally called by the native library but may be used to synthesize
		 * native events from Java without replaying them on the native system.  If
		 * you would like to send events to other applications, please use
		 * {@link #postNativeEvent},
		 * <p>
		 *
		 * <b>Note:</b> This method executes on the native system's event queue.
		 * It is imperative that all processing be off-loaded to other threads.
		 * Failure to do so might result in the delay of user input and the automatic
		 * removal of the native hook.
		 *
		 * @param event the <code>NativeInputEvent</code> sent to the registered event listeners.
		 */
		protected static void dispatchEvent(NativeInputEvent event) {
			System.out.println("Dispatching Event");
//			IceTermOptionsProvider options = IceTermOptionsProvider.getInstance();
//			GlobalScreen.postNativeEvent(new NativeKeyEvent(
//					NativeKeyEvent.NATIVE_KEY_PRESSED,
//					options.getEscapeKey().getModifiers(),
//					options.getEscapeKey().getKeyCode(),
//					options.getEscapeKey().getKeyCode(),
//					options.getescapekey().getkeychar()));



			if (eventExecutor != null) {
				System.out.println("Executing");
				eventExecutor.execute(new EventDispatchTask(event));
			}
		}
	}


	/**
	 * Enable the native hook. If the hooks is currently enabled, this function has no effect.
	 * <p>
	 * <b>Note:</b> This method will throw a <code>NativeHookException</code>
	 * if specific operating system feature is unavailable or disabled.
	 * For example: Access for assistive devices is unchecked in the Universal
	 * Access section of the System Preferences on Apple's OS X platform or
	 * <code>Load "record"</code> is missing for the xorg.conf file on
	 * Unix/Linux/Solaris platforms.
	 *
	 * @throws NativeHookException problem registering the native hook with the underlying operating system.
	 * @since 1.1
	 */
	public static void registerNativeHook() throws NativeHookException {
		if (eventExecutor != null) {
			if (! eventExecutor.isShutdown()) {
				eventExecutor.shutdown();
			}

			while (! eventExecutor.isTerminated()) {
				try {
					Thread.sleep(250);
				}
				catch (InterruptedException e) {
					log.warning(e.getMessage());
					break;
				}
			}

			eventExecutor = new DefaultDispatchService();
		}

		if (hookThread == null || !hookThread.isAlive()) {
			hookThread = new NativeHookThread();

			synchronized (hookThread) {
				hookThread.start();

				try {
					hookThread.wait();
				}
				catch (InterruptedException e) {
					throw new NativeHookException(e);
				}

				NativeHookException exception = hookThread.getException();
				if (exception != null) {
					throw exception;
				}
			}
		}
	}

	/**
	 * Disable the native hook if it is currently registered. If the native
	 * hook it is not registered the function has no effect.
	 *
	 * @throws NativeHookException hook interrupted by Java.
	 * @since 1.1
	 */
	public static void unregisterNativeHook() throws NativeHookException {
		if (isNativeHookRegistered()) {

			synchronized (hookThread) {
				try {
					hookThread.disable();
					hookThread.join();
				}
				catch (Exception e) {
					throw new NativeHookException(e.getCause());
				}
			}

			eventExecutor.shutdown();
		}
	}

	/**
	 * Returns <code>true</code> if the native hook is currently registered.
	 *
	 * @return true if the native hook is currently registered.
	 * @since 1.1
	 */
	public static boolean isNativeHookRegistered() {
		return hookThread != null && hookThread.isAlive();
	}


	/**
	 * Add a <code>NativeInputEvent</code> to the operating system's event queue.
	 * Each type of <code>NativeInputEvent</code> is processed according to its
	 * event id.
	 * <p>
	 *
	 * For both <code>NATIVE_KEY_PRESSED</code> and
	 * <code>NATIVE_KEY_RELEASED</code> events, the virtual keycode and modifier
	 * mask are used in the creation of the native event.  Please note that some
	 * platforms may generate <code>NATIVE_KEY_PRESSED</code> and
	 * <code>NATIVE_KEY_RELEASED</code> events for each required modifier.
	 * <code>NATIVE_KEY_TYPED</code> events will first translate the associated
	 * keyChar to its respective virtual code and then produce a
	 * <code>NATIVE_KEY_PRESSED</code> followed by a <code>NATuVE_KEY_RELEASED</code>
	 * event using that virtual code.  If the JNativeHook is unable to translate
	 * the keyChar to its respective virtual code, the event is ignored.
	 * <p>
	 *
	 * <code>NativeMouseEvents</code> are processed in much the same way as the
	 * <code>NativeKeyEvents</code>.  Both <code>NATIVE_MOUSE_PRESSED</code> and
	 * <code>NATIVE_MOUSE_RELEASED</code> produce events corresponding to the
	 * event's button code.  Keyboard modifiers may be used in conjunction with
	 * button press and release events, however, they might produce events for each
	 * modifier.  <code>NATIVE_MOUSE_CLICKED</code> events produce a
	 * <code>NATIVE_MOUSE_PRESSED</code> event followed by a
	 * <code>NATIVE_MOUSE_RELEASED</code> for the assigned event button.
	 * <p>
	 *
	 * <code>NATIVE_MOUSE_DRAGGED</code> and <code>NATIVE_MOUSE_MOVED</code> events
	 * are handled identically.  In order to produce a <code>NATIVE_MOUSE_DRAGGED</code>
	 * event, you must specify a button modifier mask that contains at least one
	 * button modifier and assign it to the event.  Failure to do so will produce a
	 * <code>NATIVE_MOUSE_MOVED</code> event even if the event id was set to
	 * <code>NATIVE_MOUSE_DRAGGED</code>.
	 * <p>
	 *
	 * <code>NATIVE_MOUSE_WHEEL</code> events are identical to
	 * <code>NATIVE_MOUSE_PRESSED</code> events.  Wheel events will only produce
	 * pressed events and will never produce <code>NATIVE_MOUSE_RELEASED</code>,
	 * <code>NATIVE_MOUSE_DRAGGED</code> or <code>NATIVE_MOUSE_MOVED</code>
	 *
	 * @param nativeEvent the <code>NativeInputEvent</code> sent to the native system.
	 * @since 2.0
	 */
	public static native void postNativeEvent(NativeInputEvent nativeEvent);

	/**
	 * Internal class to handle event dispatching via the executor service.
	 */
	private static class EventDispatchTask implements Runnable {
		/**
		 * The event to dispatch.
		 */
		private NativeInputEvent event;

		/**
		 * Single argumen constructor for dispatch task.
		 *
		 * @param event the <code>NativeInputEvent</code> to dispatch.
		 */
		public EventDispatchTask(NativeInputEvent event) {
			this.event = event;
		}

		public void run() {
			if (event instanceof NativeKeyEvent) {
				processKeyEvent((NativeKeyEvent) event);
			}
			else if (event instanceof NativeMouseEvent) {
			    processMouseButtonEvent((NativeMouseEvent) event);
			}
		}

		/**
		 * Processes native mouse button events by dispatching them to all registered
		 * <code>NativeMouseListener</code> objects.
		 *
		 * @param nativeEvent the <code>NativeMouseEvent</code> to dispatch.
		 * @see NativeMouseEvent
		 * @see NativeMouseListener
		 * @see #addNativeMouseListener(NativeMouseListener)
		 */
		private void processMouseButtonEvent(NativeMouseEvent nativeEvent) {
			NativeMouseListener[] listeners = eventListeners.getListeners(NativeMouseListener.class);
			System.out.println("Processing Mouse Button Event");

			for (int i = 0; i < listeners.length; i++) {
				switch (nativeEvent.getID()) {
					case NativeMouseEvent.NATIVE_MOUSE_CLICKED:
						System.out.println("NATIVE_MOUSE_CLICKED");
						listeners[i].nativeMouseClicked(nativeEvent);
						break;

					case NativeMouseEvent.NATIVE_MOUSE_PRESSED:
						System.out.println("NATIVE_MOUSE_PRESSED");
						listeners[i].nativeMousePressed(nativeEvent);
						break;

					case NativeMouseEvent.NATIVE_MOUSE_RELEASED:
						System.out.println("NATIVE_MOUSE_RELEASED");
						listeners[i].nativeMouseReleased(nativeEvent);
						break;
				}
			}
		}


		/**
		 * Processes native key events by dispatching them to all registered
		 * <code>NativeKeyListener</code> objects.
		 *
		 * @param nativeEvent the <code>NativeKeyEvent</code> to dispatch.
		 * @see NativeKeyEvent
		 * @see NativeKeyListener
		 * @see #addNativeKeyListener(NativeKeyListener)
		 */
		private void processKeyEvent(NativeKeyEvent nativeEvent) {
			NativeKeyListener[] listeners = eventListeners.getListeners(NativeKeyListener.class);

			for (int i = 0; i < listeners.length; i++) {
				switch (nativeEvent.getID()) {
					case NativeKeyEvent.NATIVE_KEY_PRESSED:
						System.out.println("NATIVE_KEY_PRESSED");
						listeners[i].nativeKeyPressed(nativeEvent);
						break;

					case NativeKeyEvent.NATIVE_KEY_TYPED:
						System.out.println("NATIVE_KEY_TYPED");
						listeners[i].nativeKeyTyped(nativeEvent);
						break;

					case NativeKeyEvent.NATIVE_KEY_RELEASED:
						System.out.println("NATIVE_KEY_RELEASED");
						listeners[i].nativeKeyReleased(nativeEvent);
						break;
				}
			}
		}
	}



	/**
	 * Set a different executor service for native event delivery.  By default,
	 * JNativeHook utilizes a single thread executor to dispatch events from
	 * the native event queue.  You may choose to use an alternative approach
	 * for event delivery by implementing an <code>ExecutorService</code>.
	 * <p>
	 * <b>Note:</b> Using null as an <code>ExecutorService</code> will cause all
	 * delivered events to be discarded until a valid <code>ExecutorService</code>
	 * is set.
	 *
	 * @param dispatcher The <code>ExecutorService</code> used to dispatch native events.
	 * @see ExecutorService
	 * @see java.util.concurrent.Executors#newSingleThreadExecutor()
	 * @since 2.0
	 */
	public static void setEventDispatcher(ExecutorService dispatcher) {
		if (GlobalScreen.eventExecutor != null) {
			GlobalScreen.eventExecutor.shutdown();
		}

		GlobalScreen.eventExecutor = dispatcher;
	}
}
