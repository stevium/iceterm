package org.iceterm.cehook.dispatcher;

// Imports.

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Swing compatible implementation of the <code>ExecutorService</code> used to dispatch native events.  This wraps
 * event dispatching with {@link java.awt.EventQueue#invokeLater}.
 *
 * @see  java.util.concurrent.ExecutorService
 */
public class SwingDispatchService extends AbstractExecutorService {
	private boolean running = false;

	public SwingDispatchService() {
		running = true;
	}

	public void shutdown() {
		running = false;
	}

	public List<Runnable> shutdownNow() {
		running = false;
		return new ArrayList<Runnable>(0);
	}

	public boolean isShutdown() {
		return !running;
	}

	public boolean isTerminated() {
		return !running;
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return true;
	}

	public void execute(Runnable r) {
		r.run();
//		SwingUtilities.invokeLater(r);
	}
}
