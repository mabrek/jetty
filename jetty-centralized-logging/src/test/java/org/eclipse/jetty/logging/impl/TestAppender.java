// ========================================================================
// Copyright (c) Webtide LLC
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
//
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
//
// The Apache License v2.0 is available at
// http://www.apache.org/licenses/LICENSE-2.0.txt
//
// You may elect to redistribute this code under either of these licenses.
// ========================================================================
package org.eclipse.jetty.logging.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Appender, records the logging events.
 */
public class TestAppender implements Appender
{
    public static class LogEvent
    {
        String date;
        int ms;
        Severity severity;
        String name;
        String message;
        Throwable t;

        public LogEvent(String date, int ms, Severity severity, String name, String message, Throwable t)
        {
            super();
            this.date = date;
            this.ms = ms;
            this.severity = severity;
            this.name = name;
            this.message = message;
            this.t = t;
        }

        @Override
        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append(severity.name()).append("|");
            buf.append(name).append("|");
            buf.append(message);
            return buf.toString();
        }
    }

    private List<LogEvent> events = new ArrayList<LogEvent>();

    public void append(String date, int ms, Severity severity, String name, String message, Throwable t)
    {
        if (name.equals("log")) // standard jetty logger
        {
            return; // skip storing it.
        }
        events.add(new LogEvent(date,ms,severity,name,message,t));
    }

    public void close() throws IOException
    {
        /* nothing to do here */
    }

    public boolean contains(LogEvent expectedEvent)
    {
        // System.out.println("Looking for: " + expectedEvent);
        for (LogEvent event : events)
        {
            // System.out.println("Event: " + event);
            if (!event.name.equals(expectedEvent.name))
            {
                continue; // not a match. skip.
            }
            if (!event.severity.equals(expectedEvent.severity))
            {
                continue; // not a match. skip.
            }
            if (event.message.equals(expectedEvent.message))
            {
                return true;
            }
        }
        return false;
    }

    public List<LogEvent> getEvents()
    {
        return events;
    }

    public void open() throws IOException
    {
        /* nothing to do here */
    }

    public void reset()
    {
        events.clear();
    }

    public void setProperty(String key, String value) throws Exception
    {
        /* nothing to do here */
    }
}
