package org.iceterm.ceintegration;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import org.apache.commons.lang.NullArgumentException;
import org.iceterm.IceTermView;
import org.iceterm.util.ToolWindowUtils;
import org.iceterm.util.User32Ext;
import org.iceterm.util.WinApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.function.Consumer;

import static com.sun.jna.Native.getComponentPointer;

/**
 * <p>This is a console emulator control that embeds a fully functional console view in a Windows Forms window. It is capable of running any console application with full interactivity and advanced console functions. Applications will detect it as an actual console and will not fall back to the output redirection mode with reduced interactivity or formatting.</p>
 * <p>The control can be used to run a console process in the console emulator. The console process is the single command executed in the control, which could be a simple executable (the console emulator is not usable after it exits), or an interactive shell like <code>cmd</code> or <code>powershell</code> or <code>bash</code>, which in turn can execute multiple commands, either buy user input or programmatically with {@link ConEmuSession#writeInputText(String)}. The console emulator is what implements the console and renders the console view in the control. A new console emulator (represented by a {@link #getRunningSession()} is {@link #Start(ConEmuStartInfo)}  started} for each console process. After the root console process terminates, the console emulator might remain open {@link ConEmuStartInfo#getConsoleProcessCommandLine()} and still present the console window, or get closed. After the console emulator exits, the control is blank until {@link #Start(ConEmuStartInfo)} spawns a new console emulator process in it. You cannot run more than one console emulator (console process) simultaneously.</p>
 */
public class ConEmuControl extends Canvas {

    private final List<StateChangedListener> stateChagedListeners = new ArrayList();
    private final List<ControlRemovedListener> controlRemovedListener = new ArrayList();
    private final ToolWindow myToolWindow;
    private final ToolWindowUtils myToolWindowUtils;
    public boolean needsFocus;

    /**
     * Enabled by default, and with all default values (runs the cmd shell).
     */
    private ConEmuStartInfo _autostartinfo;

    private boolean _isStatusbarVisible = true;

    /**
     * After the first console process exits (not session), stores it's exit code. Changes on the main thread only.
     */
    private Integer _nLastExitCode;

    /**
     * The running session, if currently running.
     */
    @Nullable
    private ConEmuSession session;

    private ConEmuStartInfo startinfo;

    public void terminate() {
        if (session != null) {
            session.close();
        }
        session = null;
    }

    public ConEmuSession getSession() {
        return session;
    }

    public ConEmuControl(ConEmuStartInfo startinfo, ToolWindow toolWindow, ToolWindowUtils toolWindowUtils) {
        this.setFocusable(true);
        this.setEnabled(true);
        this.setFocusTraversalKeysEnabled(true);
        this.startinfo = startinfo;
        this.myToolWindow = toolWindow;
        this.myToolWindowUtils = toolWindowUtils;
    }

    public GuiMacroResult setParentHWND(Pointer hwnd) {
        if (session != null) {
            GuiMacroResult guiMacroResult = session.ExecuteGuiMacroTextSync("SetParentHWND " + Pointer.nativeValue(hwnd));
            return guiMacroResult;
        }
        return null;
    }

    public void setFocus(boolean force, boolean onlyParent) {
        this.needsFocus = force;
        if (session != null) {
            Thread t = new Thread(() -> {
                try {
                    if (isForeground()) {
                        if (!onlyParent) {
                            session.ExecuteGuiMacroTextSync("SetFocus");
                        }
                        Thread.sleep(200);
                        this.myToolWindow.getComponent().requestFocus();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            t.start();
        }
    }

    public void removeFocus() {
        if (isForeground()) {
            int appThread = Kernel32.INSTANCE.GetCurrentThreadId();
            WinDef.HWND ideHwnd = new WinDef.HWND(getComponentPointer(myToolWindowUtils.getMainFrame()));
            int foregroundThread = User32Ext.INSTANCE.GetWindowThreadProcessId(ideHwnd, null);
            User32Ext.INSTANCE.AttachThreadInput(foregroundThread, appThread, true);
            User32Ext.INSTANCE.SetForegroundWindow(this.getHandle());
            User32Ext.INSTANCE.SetForegroundWindow(ideHwnd);
            User32Ext.INSTANCE.SetFocus(ideHwnd);
            User32Ext.INSTANCE.AttachThreadInput(foregroundThread, appThread, false);
            ToolWindowManager.getInstance(startinfo.getProject()).activateEditorComponent();
        }
    }

    public void resetParentHWND() {
        setParentHWND(getHandle().getPointer());
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (session == null) {
            createSession(this.startinfo);
        } else {
            this.resetParentHWND();
        }
        if (needsFocus) {
            this.myToolWindow.getComponent().requestFocus();
            this.setFocus(false, false);
        }
    }

    public void invalidateSession() {
        this.session = null;
    }

    public void setStartInfo(ConEmuStartInfo _startinfo) {
        this.startinfo = _startinfo;
    }

    public void createSession(ConEmuStartInfo startinfo) {
        session = this.Start(startinfo);
        session.addConsoleProcessExitedEventSink((source, event) -> {
            stateChanged();
        });
        session.addConsoleEmulatorClosedEventSink((source, event) -> {
            stateChanged();
        });
    }

    public ConEmuStartInfo getAutoStartInfo() {
        return _autostartinfo;
    }

    public void setAutoStartInfo(ConEmuStartInfo value) {
        if (getState() != States.Unused)
            throw new IllegalStateException("AutoStartInfo can only be changed before the first console emulator session runs in this control.");

        _autostartinfo = value;

        // Invariant: if changed to TRUE past the normal AutoStartInfo checking point
        // TODO
        // if((value != null) && (IsHandleCreated))
        if ((value != null))
            Start(value);
    }

    public boolean getIsStatusbarVisible() {
        return _isStatusbarVisible;
    }

    public void setIsStatusbarVisible(boolean value) {
        _isStatusbarVisible = value;
        if (session != null) {
            session.beginGuiMacro("Status").withParam(0).withParam(value ? 1 : 2).executeAsync();
        }
    }

    public Integer getLastExitCode() {
        // Special case for just-exited payload: user might get the payload-exited event before us and call this property to get its exit code, while we have not recorded the fresh exit code yet
        // So call into the current session and fetch the actual value, if available (no need to write to field, will update in our event handler soon)
        ConEmuSession running = session;
        if ((running != null) && (running.isConsoleProcessExited()))
            return running.getConsoleProcessExitCode();
        return _nLastExitCode; // No console emulator open or current process still running in the console emulator, use prev exit code if there were
    }

    @NotNull
    public ConEmuSession getRunningSession() {
        return session;
    }

    public States getState() {
        return session != null ? (session.isConsoleProcessExited() ? States.ConsoleEmulatorEmpty : States.ConsoleEmulatorWithConsoleProcess) : (_nLastExitCode != null ? States.Recycled : States.Unused);
    }

    public boolean getIsConsoleEmulatorOpen() {
        return session != null;
    }

    @NotNull
    public ConEmuSession Start(@NotNull ConEmuStartInfo startinfo) {
        if (startinfo == null)
            throw new NullArgumentException("startinfo");

        // Close prev session if there is one
        if (session != null)
            throw new IllegalStateException("Cannot start a new console process because another console emulator session has failed to close in due time.");

        _autostartinfo = null; // As we're starting, no more chance for an autostart
        // TODO
        // if(!getIsHandleCreated())
        // createHandle();

        // Spawn session
        session = new ConEmuSession(startinfo, new ConEmuSession.HostContext(getHandle(), getIsStatusbarVisible()));
        stateChanged();
        session.waitForConsoleEmulatorCloseAsync().continueWith(task -> {
            try {
                _nLastExitCode = session.getConsoleProcessExitCode();
            } catch (Exception ex) {
                // NOP
            }
            session = null;
            stateChanged();
            return _nLastExitCode;
        });
        return session;
    }

    private void AssertNotRunning() {
        if (session != null)
            throw new IllegalStateException("This changes is not possible wen a console process is already running.");
    }

    private HWND tryGetConEmuHwnd() {
        Pointer pConEmu = Pointer.NULL;
        HWND thisHwnd = getHandle();
        WNDENUMPROC proc = (hwnd, pointer) -> {
            Pointer.nativeValue(pointer, Pointer.nativeValue(hwnd.getPointer()));
            return false;
        };
        WinApi.EmuChildWindows(thisHwnd, proc, pConEmu);
        return new HWND(pConEmu);
    }

    public HWND getHandle() {
        try {
            return new HWND(getComponentPointer(this));
        } catch (Exception e) {
            return null;
        }
    }

    private void stateChanged() {
        for (StateChangedListener listener :
                this.stateChagedListeners) {
            listener.stateChanged();
        }
    }

    public interface StateChangedListener extends EventListener {
        void stateChanged();
    }

    public void addStateChangedListener(StateChangedListener listener) {
        this.stateChagedListeners.add(listener);
    }

    public void removeStateChangedListener(StateChangedListener listener) {
        this.stateChagedListeners.remove(listener);
    }

    public interface ControlRemovedListener extends EventListener {
        void onRemoved();
    }

    public void addControlRemovedListener(ControlRemovedListener listener) {
        this.controlRemovedListener.add(listener);
    }

    public void removeControlRemovedListener(StateChangedListener listener) {
        this.controlRemovedListener.remove(listener);
    }

    public boolean isFocused() {
        if (getHandle() == null)
            return false;

        IceTermView iceTermView = IceTermView.getInstance(startinfo.getProject());
        if (iceTermView == null) {
            return false;
        }

        HWND mainWindow = new HWND(getComponentPointer(myToolWindowUtils.getMainFrame()));
        WinDef.HWND foregroundHwnd = User32Ext.INSTANCE.GetForegroundWindow();

        WinDef.HWND conemuHwnd = getHandle();
        if (conemuHwnd.equals(foregroundHwnd) || myToolWindow.getComponent().hasFocus()) {
            return true;
        }
        return false;
    }

    public boolean isForeground() {
        if (getHandle() == null)
            return false;

        WinDef.HWND foregroundHwnd = User32Ext.INSTANCE.GetForegroundWindow();

        JRootPane conemuRootPane = ((JComponent) getParent()).getRootPane();

        if (conemuRootPane == null) {
            return false;
        }

        WinDef.HWND conEmuRootHwnd = new HWND(getComponentPointer(conemuRootPane.getParent()));

        return conEmuRootHwnd.equals(foregroundHwnd);
    }

    @Override
    public void removeNotify() {
        this.controlRemovedListener.forEach(new Consumer<ControlRemovedListener>() {
            @Override
            public void accept(ControlRemovedListener controlRemovedListener) {
                controlRemovedListener.onRemoved();
            }
        });
        super.removeNotify();
    }
}
