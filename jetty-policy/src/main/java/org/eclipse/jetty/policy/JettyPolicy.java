package org.eclipse.jetty.policy;

//========================================================================
//Copyright (c) Webtide LLC
//------------------------------------------------------------------------
//All rights reserved. This program and the accompanying materials
//are made available under the terms of the Eclipse Public License v1.0
//and Apache License v2.0 which accompanies this distribution.
//
//The Eclipse Public License is available at 
//http://www.eclipse.org/legal/epl-v10.html
//
//The Apache License v2.0 is available at
//http://www.opensource.org/licenses/apache2.0.php
//
//You may elect to redistribute this code under either of these licenses. 
//========================================================================

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.policy.loader.DefaultPolicyLoader;

/**
 * Policy implementation that will load a set of policy files and manage the mapping of permissions and protection domains 
 * 
 * The reason I created this class and added this mechanism are:
 * 
 * 1) I wanted a way to be able to follow the startup mechanic that jetty uses with jetty-start using OPTIONS=policy,default 
 * to be able to startup a security manager and policy implementation without have to rely on the existing JVM cli options
 * 2) establish a starting point to add on further functionality to permissions based security with jetty like jmx enabled 
 * permission tweaking or runtime creation and specification of policies for specific webapps
 * 3) I wanted to have support for specifying multiple policy files to source permissions from
 * 
 * Possible additions are:
 * - directories of policy file support
 * - jmx enabled a la #2 above
 * - proxying of system security policy where we can proxy access to the system policy should the jvm have been started with 
 *   one, I had support for this but ripped it out to add in again later
 * - merging of protection domains if process multiple policy files that declare permissions for the same codebase
 * - an xml policy file parser, had originally added this using modello but tore it out since it would have been
 *   a nightmare to get its dependencies through IP validation, could do this with jvm xml parser instead sometime
 * - check performance of the synch'd map I am using for the protection domain mapping
 */
public class JettyPolicy extends Policy
{
    // Policy files that are actively managed by the aggregate policy mechanism
    private final Set<String> _policies;

    private final Map<ProtectionDomain, PolicyBlock> pdMapping =
        Collections.synchronizedMap( new HashMap<ProtectionDomain, PolicyBlock>() );
    
    private final PolicyContext _context = new PolicyContext();

    // private final Scanner scanner = new Scanner();

    private boolean initialized = false;

    public JettyPolicy( Set<String> policies, Map<String,String> properties )
    {
        _policies = policies;
        _context.setProperties( properties );

        refresh();
    }

    @Override
    public PermissionCollection getPermissions( ProtectionDomain domain )
    {     
        PermissionCollection perms = new Permissions();

        for ( Iterator<ProtectionDomain> i = pdMapping.keySet().iterator(); i.hasNext(); )
        {
            ProtectionDomain pd = i.next();
            
            // System.out.println("----START----");
            // System.out.println("PDCS: " + pd.getCodeSource());
            // System.out.println("CS: " + domain.getCodeSource());

            if (pd.getCodeSource() == null || pd.getCodeSource().implies(domain.getCodeSource()) && pd.getPrincipals() == null || pd.getCodeSource().implies(domain.getCodeSource()) && validate(pd.getPrincipals(),domain.getPrincipals()))
            {         
                // gather dynamic permissions
                if ( pdMapping.get( pd ) != null )
                {
                    for ( Enumeration<Permission> e = pdMapping.get( pd ).getPermissions().elements(); e.hasMoreElements(); )
                    {
                        Permission perm = e.nextElement();
                        // System.out.println("D: " + perm);
                        perms.add(perm);
                    }
                }
            }
            // System.out.println("----STOP----");
        }
        
        return perms;
    }

    @Override
    public PermissionCollection getPermissions( CodeSource codesource )
    {
        PermissionCollection perms = new Permissions();

        for ( Iterator<ProtectionDomain> i = pdMapping.keySet().iterator(); i.hasNext(); )
        {
            ProtectionDomain pd = i.next();

            if ( pd.getCodeSource() == null || pd.getCodeSource().implies( codesource ) )
            {

                // System.out.println("----START----");
                // System.out.println("PDCS: " + pd.getCodeSource());
                // System.out.println("CS: " + codesource);

                // gather dynamic permissions
                if ( pdMapping.get( pd ) != null )
                {
                    for ( Enumeration<Permission> e = pdMapping.get( pd ).getPermissions().elements(); e.hasMoreElements(); )
                    {
                        Permission perm = e.nextElement();
                        // System.out.println("D: " + perm);
                        perms.add(perm);
                    }
                }
                // System.out.println("----STOP----");
            }
        }

        return perms;
    }

    private static boolean validate( Principal[] permCerts, Principal[] classCerts )
    {
        if ( classCerts == null )
        {
            return false;
        }
        
        for ( int i = 0; i < permCerts.length; ++i )
        {
            boolean found = false;           
            for ( int j = 0; j < classCerts.length; ++j )
            {
                if ( permCerts[i].equals( classCerts[j] ) )
                {
                    found = true;
                    break;
                }
            }
            // if we didn't find the permCert in the classCerts then we don't match up
            if ( found == false )
            {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public void refresh()
    {

        try
        {
            if (!initialized)
            {
                initialize();
            }

            // System.out.println("refreshing policy files");

            pdMapping.clear();

            for ( Iterator<String> i = _policies.iterator(); i.hasNext(); )
            {
                File policyFile = new File( i.next() );              
                pdMapping.putAll( DefaultPolicyLoader.load( new FileInputStream( policyFile ), _context ) );
            }
            
            // System.setSecurityManager(null);
            // Policy.setPolicy(null);

            // Policy.setPolicy(this);
            // System.setSecurityManager(new SecurityManager());

            // for (Iterator<ProtectionDomain> i = pdMapping.keySet().iterator(); i.hasNext();)
            // {
            // System.out.println(i.next().toString());
            // }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    /**
     * TODO make this optional
     */
    private void initialize() throws Exception
    {
        /*
         * List scanDirs = new ArrayList();
         * 
         * for (Iterator<String> i = _policies.iterator(); i.hasNext();) { File policyFile = new File(i.next()); scanDirs.add(policyFile.getParentFile()); }
         * 
         * scanner.addListener(new Scanner.DiscreteListener() {
         * 
         * public void fileRemoved(String filename) throws Exception { // TODO Auto-generated method stub
         * 
         * }
         * 
         * public void fileChanged(String filename) throws Exception { refresh(); }
         * 
         * public void fileAdded(String filename) throws Exception { // TODO Auto-generated method stub
         * 
         * } });
         * 
         * scanner.setScanDirs(scanDirs); scanner.start(); scanner.setScanInterval(10);
         */
        initialized = true;
    }

    public void dump( PrintStream out )
    {
        PrintWriter write = new PrintWriter( out );
        write.println("dumping policy settings");
        
        for( Iterator<ProtectionDomain> i = pdMapping.keySet().iterator(); i.hasNext(); ) 
        {
            ProtectionDomain domain = i.next();
            PolicyBlock block = pdMapping.get( domain );
            write.println(domain.toString());   
        }
        write.flush();
    }
}
