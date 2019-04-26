package org.iceterm.integration;

/**
 * Controls behavior of the console emulator when the console process running inside it terminates.
 */
public enum WhenConsoleProcessExits {
    CloseConsoleEmulator,
    KeepConsoleEmulator,
    KeepConsoleEmulatorAndShowMessage
}
