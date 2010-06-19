// ========================================================================
// Copyright (c) 2009 Intalio, Inc.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// Contributors:
//    Hugues Malphettes - initial API and implementation
// ========================================================================
package org.eclipse.jetty.osgi.boot;

/**
 * 
 */
public class OSGiWebappConstants
{
    /** url scheme to deploy war file as bundled webapp */
    public static final String RFC66_WAR_URL_SCHEME = "war";

    /**
     * Name of the header that defines the context path for the embedded webapp.
     */
    public static final String RFC66_WEB_CONTEXTPATH = "Web-ContextPath";

    /**
     * Name of the header that defines the path to the folder where the jsp
     * files are extracted.
     */
    public static final String RFC66_JSP_EXTRACT_LOCATION = "Jsp-ExtractLocation";

    /** Name of the servlet context attribute that points to the bundle context. */
    public static final String RFC66_OSGI_BUNDLE_CONTEXT = "osgi-bundlecontext";

    /** List of relative pathes within the bundle to the jetty context files. */
    public static final String JETTY_CONTEXT_FILE_PATH = "Jetty-ContextFilePath";

    /** path within the bundle to the folder that contains the basic resources. */
    public static final String JETTY_WAR_FOLDER_PATH = "Jetty-WarFolderPath";

    // OSGi ContextHandler service properties.
    /** web app context path */
    public static final String SERVICE_PROP_CONTEXT_PATH = "contextPath";

    /** Path to the web application base folderr */
    public static final String SERVICE_PROP_WAR = "war";

    /** Extra classpath */
    public static final String SERVICE_PROP_EXTRA_CLASSPATH = "extraClasspath";

    /** jetty context file path */
    public static final String SERVICE_PROP_CONTEXT_FILE_PATH = "contextFilePath";

    /** web.xml file path */
    public static final String SERVICE_PROP_WEB_XML_PATH = "webXmlFilePath";

    /** defaultweb.xml file path */
    public static final String SERVICE_PROP_DEFAULT_WEB_XML_PATH = "defaultWebXmlFilePath";

    /**
     * path to the base folder that overrides the computed bundle installation
     * location if not null useful to install webapps or jetty context files
     * that are in fact not embedded in a bundle
     */
    public static final String SERVICE_PROP_BUNDLE_INSTALL_LOCATION_OVERRIDE = "thisBundleInstall";

    // sys prop config of jetty:
    /**
     * contains a comma separated list of pathes to the etc/jetty-*.xml files
     * used to configure jetty. By default the value is 'etc/jetty.xml' when the
     * path is relative the file is resolved relatively to jettyhome.
     */
    public static final String SYS_PROP_JETTY_ETC_FILES = "jetty.etc.files";
    
    //for managed jetty instances, name of the configuration parameters
    /**
     * PID of the jetty servers's ManagedFactory
     */
    public static final String MANAGED_JETTY_SERVER_FACTORY_PID = "org.eclipse.jetty.osgi.boot.managedserverfactory";
    
    /**
     * The associated value of that configuration parameter is the name under which this
     * instance of the jetty server is tracked.
     * When a ContextHandler is deployed and it specifies the managedServerName property, it is deployed
     * on the corresponding jetty managed server or it throws an exception: jetty server not available.
     */
    public static final String MANAGED_JETTY_SERVER_NAME = "managedServerName";
    
    /**
     * List of URLs to the jetty.xml files that configure the server.
     */
    public static final String MANAGED_JETTY_XML_CONFIG_URLS = SYS_PROP_JETTY_ETC_FILES;
    
    /**
     * List of URLs to the folders where the legacy J2EE shared libraries are stored aka lib/ext, lib/jsp etc.
     */
    public static final String MANAGED_JETTY_SHARED_LIB_URLS = "managedJettySharedLibUrls";
    
}
