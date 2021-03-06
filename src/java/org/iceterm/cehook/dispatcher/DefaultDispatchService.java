package org.iceterm.cehook.dispatcher;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of the <code>ExecutorService</code> used to dispatch native events.  This is effectively
 * the same as calling {@link java.util.concurrent.Executors#newSingleThreadExecutor}.
 * <p>
 *
 * @see  java.util.concurrent.ExecutorService
 * @see  org.iceterm.cehook.GlobalScreen#setEventDispatcher
 */
public class DefaultDispatchService extends ThreadPoolExecutor {
	/**
	 * Instantiates a new default dispatch service using a single thread.
	 */
	public DefaultDispatchService() {
		super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("JNativeHook Dispatch Thread");
				t.setDaemon(true);

				return t;
			}
		});
	}
}
