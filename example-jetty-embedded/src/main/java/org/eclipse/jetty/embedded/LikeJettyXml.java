// ========================================================================
// Copyright (c) 2006-2009 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.embedded;

import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.deploy.ContextDeployer;
import org.eclipse.jetty.deploy.WebAppDeployer;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class LikeJettyXml
{
    public static void main(String[] args)
        throws Exception
    {
        String jetty_home = System.getProperty("jetty.home",".");
        
        Server server = new Server();
        
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(100);
        server.setThreadPool(threadPool);
             
        Connector connector=new SelectChannelConnector();
        connector.setPort(8080);
        connector.setMaxIdleTime(30000);
        server.setConnectors(new Connector[]{connector});
        
        HandlerCollection handlers = new HandlerCollection();
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        handlers.setHandlers(new Handler[]{contexts,new DefaultHandler(),requestLogHandler});
        server.setHandler(handlers);
        
        ContextDeployer deployer0 = new ContextDeployer();
        deployer0.setContexts(contexts);
        deployer0.setConfigurationDir(jetty_home+"/contexts");
        deployer0.setScanInterval(1);
        server.addBean(deployer0);   
        
        WebAppDeployer deployer1 = new WebAppDeployer();
        deployer1.setContexts(contexts);
        deployer1.setWebAppDir(jetty_home+"/webapps");
        deployer1.setParentLoaderPriority(false);
        deployer1.setExtract(true);
        deployer1.setAllowDuplicates(false);
        deployer1.setDefaultsDescriptor(jetty_home+"/etc/webdefault.xml");
        server.addBean(deployer1);
          
        HashLoginService login = new HashLoginService();
        login.setName("Test Realm");
        login.setConfig(jetty_home+"/etc/realm.properties");
        server.addBean(login);
        
        NCSARequestLog requestLog = new NCSARequestLog(jetty_home+"/logs/jetty-yyyy_mm_dd.log");
        requestLog.setExtended(false);
        requestLogHandler.setRequestLog(requestLog);
        
        server.setStopAtShutdown(true);
        server.setSendServerVersion(true);
        
        server.start();
        server.join();
    }
    
}