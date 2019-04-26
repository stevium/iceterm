package org.iceterm.integration;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import org.iceterm.util.WinApi;
import org.iceterm.util.tasks.Continuation;
import org.iceterm.util.tasks.Task;
import org.apache.commons.lang.NullArgumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.awt.windows.WComponentPeer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * <p>This is a console emulator control that embeds a fully functional console view in a Windows Forms window. It is capable of running any console application with full interactivity and advanced console functions. Applications will detect it as an actual console and will not fall back to the output redirection mode with reduced interactivity or formatting.</p>
 * <p>The control can be used to run a console process in the console emulator. The console process is the single command executed in the control, which could be a simple executable (the console emulator is not usable after it exits), or an interactive shell like <code>cmd</code> or <code>powershell</code> or <code>bash</code>, which in turn can execute multiple commands, either buy user input or programmatically with {@link ConEmuSession#writeInputText(String)}. The console emulator is what implements the console and renders the console view in the control. A new console emulator (represented by a {@link #getRunningSession()} is {@link #Start(ConEmuStartInfo)}  started} for each console process. After the root console process terminates, the console emulator might remain open {@link ConEmuStartInfo#getConsoleProcessCommandLine()} and still present the console window, or get closed. After the console emulator exits, the control is blank until {@link #Start(ConEmuStartInfo)} spawns a new console emulator process in it. You cannot run more than one console emulator (console process) simultaneously.</p>
 */
public class ConEmuControl extends Canvas {
    /**
     * Enabled by default, and with all default values (runs the cmd shell).
     */
    private ConEmuStartInfo _autostartinfo = new ConEmuStartInfo();

    private boolean _isStatusbarVisible = true;

    /**
     * After the first console process exits (not session), stores it's exit code. Changes on the main thread only.
     */
    private Integer _nLastExitCode;

    /**
     * The running session, if currently running.
     */
    @Nullable
    private ConEmuSession _running;

    private List<StateChagedListener> stateChagedListeners = new ArrayList();
    private ConEmuSession session;
    private ConEmuStartInfo _startinfo;

    public ConEmuSession getSession() {
        return _running;
    }

    public ConEmuControl(ConEmuStartInfo startinfo)
    {
        addFocusListener(focusGained);
        this._startinfo = startinfo;
        this.setBackground(Color.darkGray);
    }

    FocusAdapter focusGained = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            super.focusGained(e);
//            HWND hwnd = tryGetConEmuHwnd();
//            WinApi.SetFocus(hwnd);
        }
    };

    public void setParentHWND(long hwnd) {
        if(_running!= null)
            _running.ExecuteGuiMacroTextSync("SetParentHWND " + hwnd);
    }

    public void resetParentHWND() {
        GuiMacroResult guiMacroResult = _running.ExecuteGuiMacroTextSync("SetParentHWND " + Pointer.nativeValue(getHandle().getPointer()));
    }

    @Override
    public void doLayout() {
        super.doLayout();
//        HWND hwnd = tryGetConEmuHwnd();
//        WinApi.MoveWindow(hwnd, 0, 0, getWidth(), getHeight(), true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if(session == null) {
            session = this.Start(_startinfo);
            session.addConsoleProcessExitedEventSink((source, event) -> {
                JOptionPane.showMessageDialog(null, "exited");
            });
            session.addConsoleEmulatorClosedEventSink((source, event) -> {
                JOptionPane.showMessageDialog(null, "closed");
            });
        } else {
            this.resetParentHWND();
        }
//        if(_running != null)
//            return;
//        Rectangle clipBounds = g.getClipBounds();
//        g.setColor(Color.darkGray);
//        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
    }

    public ConEmuStartInfo getAutoStartInfo()
    {
        return _autostartinfo;
    }

    public void setAutoStartInfo(ConEmuStartInfo value)
    {
        if(getState() != States.Unused)
            throw new IllegalStateException("AutoStartInfo can only be changed before the first console emulator session runs in this control.");

        _autostartinfo = value;

        // Invariant: if changed to TRUE past the normal AutoStartInfo checking point
        // TODO
        // if((value != null) && (IsHandleCreated))
        if((value != null))
            Start(value);
    }

    public boolean getIsStatusbarVisible()
    {
        return _isStatusbarVisible;
    }

    public void setIsStatusbarVisible(boolean value)
    {
        _isStatusbarVisible = value;
        if(_running != null) {
            _running.beginGuiMacro("Status").withParam(0).withParam(value ? 1 : 2).executeAsync();
        }
    }

    public Integer getLastExitCode()
    {
        // Special case for just-exited payload: user might get the payload-exited event before us and call this property to get its exit code, while we have not recorded the fresh exit code yet
        // So call into the current session and fetch the actual value, if available (no need to write to field, will update in our event handler soon)
        ConEmuSession running = _running;
        if((running != null) && (running.isConsoleProcessExited()))
            return running.getConsoleProcessExitCode();
        return _nLastExitCode; // No console emulator open or current process still running in the console emulator, use prev exit code if there were
    }

    @NotNull
    public ConEmuSession getRunningSession()
    {
        return _running;
    }

    public States getState()
    {
        return _running != null ? (_running.isConsoleProcessExited() ? States.ConsoleEmulatorEmpty : States.ConsoleEmulatorWithConsoleProcess) : (_nLastExitCode != null ? States.Recycled : States.Unused);
    }

    public boolean getIsConsoleEmulatorOpen()
    {
        return _running != null;
    }

    @NotNull
    public ConEmuSession Start(@NotNull ConEmuStartInfo startinfo)
    {
        if(startinfo == null)
            throw new NullArgumentException("startinfo");

        // Close prev session if there is one
        if(_running != null)
            throw new IllegalStateException("Cannot start a new console process because another console emulator session has failed to close in due time.");

        _autostartinfo = null; // As we're starting, no more chance for an autostart
        // TODO
        // if(!getIsHandleCreated())
        // createHandle();

        // Spawn session
        ConEmuSession session = new ConEmuSession(startinfo, new ConEmuSession.HostContext(getHandle(), getIsStatusbarVisible()));
        _running = session;
        stateChanged();
        session.waitForConsoleEmulatorCloseAsync().continueWith(new Continuation() {
            @Override
            public Object then(Task task) throws Exception {
                try {
                    _nLastExitCode = _running.getConsoleProcessExitCode();
                }
                catch (Exception ex)
                {
                    // NOP
                }
                _running = null;
                stateChanged();
                return _nLastExitCode;
            }
        });


        return session;
    }

    private void AssertNotRunning()
    {
        if(_running != null)
            throw new IllegalStateException("This changes is not possible wen a console process is already running.");
    }

    private HWND tryGetConEmuHwnd()
    {
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
        return new HWND(Pointer.createConstant(((WComponentPeer) this.getPeer()).getHWnd()));
    }

    private void stateChanged() {
        for (StateChagedListener listener :
                this.stateChagedListeners) {
            listener.stateChanged();
        }
    }

    public interface StateChagedListener extends EventListener {
        void stateChanged();
    }

    public void addStateChangedListener(StateChagedListener listener) {
        this.stateChagedListeners.add(listener);
    }

    public void removeStateChangedListener(StateChagedListener listener) {
        this.stateChagedListeners.remove(listener);
    }
}
