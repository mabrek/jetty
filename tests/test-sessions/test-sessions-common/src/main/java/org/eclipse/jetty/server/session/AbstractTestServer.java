// ========================================================================
// Copyright 2004-2010 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses.
// ========================================================================

package org.eclipse.jetty.server.session;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;


/**
 * AbstractTestServer
 *
 *
 */
public abstract class AbstractTestServer
{
    protected final Server _server;
    protected final int _maxInactivePeriod;
    protected final int _scavengePeriod;
    protected final ContextHandlerCollection _contexts;
    protected SessionIdManager _sessionIdManager;

    public AbstractTestServer(int port)
    {
        this(port, 30, 10);
    }

    public AbstractTestServer(int port, int maxInactivePeriod, int scavengePeriod)
    {
        _server = new Server(port);
        _maxInactivePeriod = maxInactivePeriod;
        _scavengePeriod = scavengePeriod;
        _contexts = new ContextHandlerCollection();
        _sessionIdManager = newSessionIdManager();
    }


    public abstract SessionIdManager newSessionIdManager();
    public abstract AbstractSessionManager newSessionManager();
    public abstract SessionHandler newSessionHandler(SessionManager sessionManager);


    public void start() throws Exception
    {
        // server -> contexts collection -> context handler -> session handler -> servlet handler
        _server.setHandler(_contexts);
        _server.start();
    }

    public ServletContextHandler addContext(String contextPath)
    {
        ServletContextHandler context = new ServletContextHandler(_contexts, contextPath);

        AbstractSessionManager sessionManager = newSessionManager();
        sessionManager.setIdManager(_sessionIdManager);
        sessionManager.setMaxInactiveInterval(_maxInactivePeriod);

        SessionHandler sessionHandler = newSessionHandler(sessionManager);
        sessionManager.setSessionHandler(sessionHandler);
        context.setSessionHandler(sessionHandler);

        return context;
    }

    public void stop() throws Exception
    {
        _server.stop();
    }

    public WebAppContext addWebAppContext(String warPath, String contextPath)
    {
        WebAppContext context = new WebAppContext(_contexts, warPath, contextPath);

        AbstractSessionManager sessionManager = newSessionManager();
        sessionManager.setIdManager(_sessionIdManager);
        sessionManager.setMaxInactiveInterval(_maxInactivePeriod);

        SessionHandler sessionHandler = newSessionHandler(sessionManager);
        sessionManager.setSessionHandler(sessionHandler);
        context.setSessionHandler(sessionHandler);

        return context;
    }
}
