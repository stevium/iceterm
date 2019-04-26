package org.iceterm.integration;

import org.apache.commons.lang.NullArgumentException;

import java.io.UnsupportedEncodingException;
import java.util.EventObject;

public class AnsiStreamChunkEvent extends EventObject {

    private final byte[] chunk;

    public AnsiStreamChunkEvent(byte[] chunk) {
        super(chunk);

        if (chunk == null)
            throw new NullArgumentException("chunk");
        if (chunk.length == 0)
            throw new IndexOutOfBoundsException("Empty data is not a valid stream chunk");
        this.chunk = chunk;
    }

    public byte[] getChunk() {
        return chunk;
    }

    public String GetMbcsText() {
        String defaultCharacterEncoding = System.getProperty("file.encoding");
        return GetText(chunk, defaultCharacterEncoding);
    }

    public String GetText(String encoding) {
        return GetText(chunk, encoding);
    }

    public String GetText(byte[] chunk, String encoding) {
        try {
            return new String(chunk, encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        try {
            return chunk.length + " (bytes) " + GetMbcsText();
        } catch (Exception ex) {
            return chunk.length + " (bytes) Error getting chunk text. " + ex.toString();
        }
    }
}
