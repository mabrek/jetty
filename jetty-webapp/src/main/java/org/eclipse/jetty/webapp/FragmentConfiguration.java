// ========================================================================
// Copyright (c) 2009 Mort Bay Consulting Pty. Ltd.
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


package org.eclipse.jetty.webapp;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;

/**
 * FragmentConfiguration
 *
 * Process web-fragments in jars
 */
public class FragmentConfiguration implements Configuration
{
    public void preConfigure(WebAppContext context) throws Exception
    {
        WebXmlProcessor processor = (WebXmlProcessor)context.getAttribute(WebXmlProcessor.__web_processor); 
        if (processor == null)
        {
            processor = new WebXmlProcessor (context);
            context.setAttribute(WebXmlProcessor.__web_processor, processor);
        }
        
        //parse web-fragment.xmls
        parseWebFragments(context, processor);
        
        //TODO for jetty-8/servletspec 3 we will need to merge the parsed web fragments into the 
        //effective pom in this preConfigure step
    }
    
    public void configure(WebAppContext context) throws Exception
    {
        //TODO for jetty-8/servletspec3 the fragments will not be separately processed here, but
        //will be done by webXmlConfiguration when it processes the effective merged web.xml
        WebXmlProcessor processor = (WebXmlProcessor)context.getAttribute(WebXmlProcessor.__web_processor); 
        if (processor == null)
        {
            processor = new WebXmlProcessor (context);
            context.setAttribute(WebXmlProcessor.__web_processor, processor);
        }
        processor.processFragments();
    }

    public void deconfigure(WebAppContext context) throws Exception
    {
        // TODO Auto-generated method stub

    }

    public void postConfigure(WebAppContext context) throws Exception
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Look for any web.xml fragments in META-INF of jars in WEB-INF/lib
     * 
     * @throws Exception
     */
    public void parseWebFragments (final WebAppContext context, final WebXmlProcessor processor) throws Exception
    {
        // Check to see if a specific search pattern has been set.
        String tmp = (String) context.getInitParameter("org.eclipse.jetty.webapp.WebXmlFragmentPattern");
        Pattern webFragPattern = (tmp == null ? null : Pattern.compile(tmp));

        List<URL> urls = (List<URL>)context.getAttribute(MetaInfConfiguration.__webFragJars);
        
        JarScanner fragScanner = new JarScanner()
        {
            public void processEntry(URL jarUrl, JarEntry entry)
            {
                try
                {
                    String name = entry.getName();
                    if (name.toLowerCase().equals("meta-inf/web-fragment.xml"))
                    {
                        Resource webXmlFrag = context.newResource("jar:" + jarUrl + "!/" + name);
                        Log.debug("web.xml fragment found {}", webXmlFrag);
                        // Process web.xml
                        // web-fragment
                        // servlet
                        // servlet-mapping
                        // filter
                        // filter-mapping
                        // listener
                        processor.parseFragment(webXmlFrag.getURL());      
                    }
                }
                catch (Exception e)
                {
                    Log.warn("Problem processing jar entry " + entry, e);
                }
            }
        };

        //process only the jars that have web fragments in them, according to the pattern provided
        if (urls != null)
            fragScanner.scan(webFragPattern, urls.toArray(new URL[urls.size()]), true);
        else
        {
            if (Log.isDebugEnabled()) Log.debug("No jars with web-fragments");
        }
    }

}