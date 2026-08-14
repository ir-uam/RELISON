/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures everything written to the standard output and error streams into a bounded, in-memory ring buffer, so the
 * debug console in the frontend can display it. Only installed when the server is launched with {@code --debug};
 * normal runs pay no cost.
 *
 * <p>Each captured line is assigned a monotonically increasing index. The frontend polls with the index of the last
 * line it has seen ({@code since}) and receives the newer lines plus the current total, so it can append incrementally
 * even though the buffer discards its oldest lines once full.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class LogCapture
{
    /** One captured line together with the stream it came from ({@code "out"} or {@code "err"}). */
    private static final class Entry
    {
        final String stream;
        final String text;

        Entry(String stream, String text)
        {
            this.stream = stream;
            this.text = text;
        }
    }

    /** The most recent lines, oldest first, capped at {@link #maxLines}. */
    private final Deque<Entry> buffer = new ArrayDeque<>();
    /** Maximum number of lines kept in memory. */
    private final int maxLines;
    /** Number of lines that have been evicted from the front of the buffer (so indices stay absolute). */
    private long dropped = 0;

    /**
     * @param maxLines the maximum number of lines to keep in memory (older lines are discarded).
     */
    public LogCapture(int maxLines)
    {
        this.maxLines = Math.max(1, maxLines);
    }

    /** Appends one captured line from the given stream, evicting the oldest if the buffer is full. */
    public synchronized void addLine(String stream, String line)
    {
        buffer.addLast(new Entry(stream, line));
        while (buffer.size() > maxLines)
        {
            buffer.removeFirst();
            dropped++;
        }
    }

    /**
     * Returns the captured lines with an absolute index at or above {@code cursor}, together with the new cursor (the
     * total number of lines ever captured) and whether some requested lines had already been evicted.
     * @param cursor the absolute index the caller has already seen (0 to get everything currently buffered).
     * @return a map with {@code lines} (the new lines), {@code cursor} (the new absolute index) and {@code dropped}
     *         (true when lines were evicted before the caller could read them).
     */
    public synchronized Map<String, Object> since(long cursor)
    {
        long total = dropped + buffer.size();
        long from = Math.max(cursor, dropped);   // clamp: the caller may have fallen behind the evicted window
        List<Map<String, String>> out = new ArrayList<>();
        long idx = dropped;
        for (Entry e : buffer)
        {
            if (idx >= from)
            {
                Map<String, String> line = new LinkedHashMap<>();
                line.put("stream", e.stream);
                line.put("text", e.text);
                out.add(line);
            }
            idx++;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lines", out);
        m.put("cursor", total);
        m.put("dropped", cursor < dropped);
        return m;
    }

    /**
     * Tees the standard output and error streams so their bytes still reach the console but are also captured here.
     * @param capture the buffer to feed.
     */
    public static void install(LogCapture capture)
    {
        System.setOut(tee(System.out, capture, "out"));
        System.setErr(tee(System.err, capture, "err"));
    }

    /** Wraps a print stream so its bytes are written through and also accumulated into {@code capture}, line by line. */
    private static PrintStream tee(PrintStream original, LogCapture capture, String stream)
    {
        OutputStream os = new OutputStream()
        {
            private final ByteArrayOutputStream line = new ByteArrayOutputStream();

            @Override
            public synchronized void write(int b) throws IOException
            {
                original.write(b);
                consume(b);
            }

            @Override
            public synchronized void write(byte[] b, int off, int len) throws IOException
            {
                original.write(b, off, len);
                for (int i = off; i < off + len; i++) consume(b[i]);
            }

            private void consume(int b)
            {
                int c = b & 0xff;
                if (c == '\n')
                {
                    capture.addLine(stream, new String(line.toByteArray(), StandardCharsets.UTF_8));
                    line.reset();
                }
                else if (c != '\r')
                {
                    line.write(c);
                }
            }

            @Override
            public void flush() throws IOException
            {
                original.flush();
            }
        };
        try
        {
            return new PrintStream(os, true, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return new PrintStream(os, true);
        }
    }
}
