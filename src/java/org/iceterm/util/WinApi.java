package org.iceterm.util;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.iceterm.util.tasks.Task;
import org.iceterm.util.tasks.TaskCompletionSource;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public final class WinApi {

    private static int SYNCHRONIZE = 1048576;
    private static User32 user32 = User32.INSTANCE;
    private static Kernel32 kernel32 = Kernel32.INSTANCE;

    public static boolean EmuChildWindows(HWND lpEnumFunc, WinUser.WNDENUMPROC wndEnumProc, Pointer lParam) {
        return user32.EnumChildWindows(lpEnumFunc, wndEnumProc, lParam);
    }

    public static boolean FreeLibrary(HMODULE hModule) {
        return kernel32.FreeLibrary(hModule);
    }

    public static FOREIGN_THREAD_START_ROUTINE GetProcAddress(HMODULE hModule, String lpProcName) {
        HMODULE module = Kernel32.INSTANCE.GetModuleHandle("KERNEL32");

        FOREIGN_THREAD_START_ROUTINE address = Kernel32MissingFunctions.INSTANCE.GetProcAddress(module, lpProcName);
        return address;
    }

    public static HMODULE LoadLibrary(String libname) {
        return kernel32.LoadLibraryEx(libname, null, 0);
    }

    public static boolean MoveWindow(HWND hWnd, int X, int Y, int nWidth, int nHeight, boolean bRepaint) {
        return user32.MoveWindow(hWnd, X, Y, nWidth, nHeight, bRepaint);
    }

    public static HANDLE OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId) {
        return kernel32.OpenProcess(dwDesiredAccess, bInheritHandle, dwProcessId);
    }

    public static HANDLE SetFocus(HWND hWnd) {
        return user32.SetFocus(hWnd);
    }

    public static class Helpers {
        @NotNull
        public static Task<Boolean> WaitForProcessExitAsync(int pid) {
            HANDLE hProcess = OpenProcess(SYNCHRONIZE, false, pid);
            if (hProcess == null)
                return Task.forResult(false);

            TaskCompletionSource<Boolean> tasker = new TaskCompletionSource<>();

            Thread t = new Thread(() -> {
                try {
                    kernel32.WaitForSingleObject(hProcess, Kernel32.INFINITE);
                    tasker.setResult(true);
                } catch (Exception e) {
                    tasker.setError(e);
                }
            });

            t.start();
            return tasker.getTask();
        }

        public static int getProcessId(Process process) {
            if (process.getClass().getName().equals("java.lang.Win32Process") ||
                    process.getClass().getName().equals("java.lang.ProcessImpl")) {
                try {
                    Field f = process.getClass().getDeclaredField("handle");
                    f.setAccessible(true);
                    long handl = f.getLong(process);

                    Kernel32 kernel = Kernel32.INSTANCE;
                    HANDLE handle = new HANDLE();
                    handle.setPointer(Pointer.createConstant(handl));
                    return kernel.GetProcessId(handle);
                } catch (Throwable e) {
                }
            }
            return -1;
        }
    }

    private interface Kernel32MissingFunctions extends StdCallLibrary {

        Kernel32MissingFunctions INSTANCE = (Kernel32MissingFunctions) Native.loadLibrary("kernel32",
                Kernel32MissingFunctions.class, W32APIOptions.UNICODE_OPTIONS);

        public FOREIGN_THREAD_START_ROUTINE GetProcAddress(HMODULE hModule, String lpProcName);

    }
}
