package org.iceterm.integration;

import org.apache.commons.lang.NullArgumentException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class AnsiLog {
    private final File directory;
    private File logFile;
    private FileInputStream fInputStream;

    private boolean isDisposed;
    private AnsiStreamChunkEvent args;

    private List<AnsiStreamChunkReceivedListener> ansiStreamChunkReceivedListeners = new ArrayList<>();

    public AnsiLog(File directory) {
        if (directory == null)
            throw new NullArgumentException("directory");

        this.directory = directory;

        directory.mkdirs();
    }

    public void dispose() {
        pumpStream();
        isDisposed = true;
        try {
            if(fInputStream != null)
                fInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fInputStream = null;
    }

    public void pumpStream() {
        if (isDisposed)
            return;
        fInputStream = fInputStream != null ? fInputStream : FindAnsiLogFile();

        if (fInputStream == null)
            return;

        long length = logFile.length();

        try {

            long position = fInputStream.getChannel().position();
            if (position > length)
                return;

            byte[] buffer = new byte[Math.toIntExact(length - position)];
            int nRead = fInputStream.read(buffer, 0, buffer.length);
            if (nRead <= 0)
                return;

            if (nRead < buffer.length) {
                byte[] subbuffer = new byte[nRead];
                System.arraycopy(buffer, 0, subbuffer, 0, nRead);
                args = new AnsiStreamChunkEvent(subbuffer);
            } else
                args = new AnsiStreamChunkEvent(buffer);

        } catch (IOException e) {
            e.printStackTrace();
        }

        ansiStreamChunkRecieved(this, args);
    }

    private void ansiStreamChunkRecieved(Object source, AnsiStreamChunkEvent event) {
        if (ansiStreamChunkReceivedListeners != null)
            for (AnsiStreamChunkReceivedListener l : ansiStreamChunkReceivedListeners) {
                l.chunkReceived(source, event);
            }
    }

    private FileInputStream FindAnsiLogFile() {
        File[] logFiles = directory.listFiles((dir, name) -> name.startsWith("ConEmu") && name.endsWith("log"));
        for (File fLog :
                logFiles) {
            try {
                logFile = fLog;
                return new FileInputStream(fLog);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void addAnsiStreamChunkEventListener(AnsiStreamChunkReceivedListener l) {
        ansiStreamChunkReceivedListeners.add(l);
    }

    public void removeAnsiStreamChunkEventListener(AnsiStreamChunkReceivedListener l) {
        ansiStreamChunkReceivedListeners.remove(l);
    }

    public File getDirectory() {
        return directory;
    }

    public interface AnsiStreamChunkReceivedListener extends EventListener {
        void chunkReceived(Object source, AnsiStreamChunkEvent event);
    }
}
