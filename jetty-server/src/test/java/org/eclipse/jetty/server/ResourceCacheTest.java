// ========================================================================
// Copyright (c) 2004-2009 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.server;

import java.io.File;
import java.io.FileOutputStream;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.ResourceCache.Content;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResourceCacheTest
{
    private Resource directory;
    private File[] files=new File[10];
    private String[] names=new String[files.length];
    private ResourceCache cache; 
    private ResourceFactory factory;

    @Before
    public void init() throws Exception
    {
        for (int i=0;i<files.length;i++)
        {
            files[i]=File.createTempFile("R-"+i+"-",".txt");
            files[i].deleteOnExit();
            names[i]=files[i].getName();
            FileOutputStream out = new FileOutputStream(files[i]);
            for (int j=0;j<(i*10-1);j++)
                out.write(' ');
            out.write('\n');
            out.close();
        }

        directory=Resource.newResource(files[0].getParentFile().getAbsolutePath());

        factory = new ResourceFactory()
        {
            public Resource getResource(String path)
            {
                try
                {
                    return directory.addPath(path);
                }
                catch(Exception e)
                {
                    return null;
                }
            }

        };
        
        cache=new ResourceCache(factory,new MimeTypes(),false);
        
        cache.setMaxCacheSize(95);
        cache.setMaxCachedFileSize(85);
        cache.setMaxCachedFiles(4);
    }

    @After
    public void destroy() throws Exception
    {
        cache.flushCache();
    }

    @Test
    public void testResourceCache() throws Exception
    {
        assertTrue(cache.lookup("does not exist")==null);
        assertTrue(cache.lookup(names[9])==null);

        Content content;
        content=cache.lookup(names[8]);
        assertTrue(content!=null);
        assertEquals(80,content.getContentLength());

        assertEquals(80,cache.getCachedSize());
        assertEquals(1,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[1]);
        assertEquals(90,cache.getCachedSize());
        assertEquals(2,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[2]);
        assertEquals(30,cache.getCachedSize());
        assertEquals(2,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[3]);
        assertEquals(60,cache.getCachedSize());
        assertEquals(3,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[4]);
        assertEquals(90,cache.getCachedSize());
        assertEquals(3,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[5]);
        assertEquals(90,cache.getCachedSize());
        assertEquals(2,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[6]);
        assertEquals(60,cache.getCachedSize());
        assertEquals(1,cache.getCachedFiles());

        Thread.sleep(2);
        
        FileOutputStream out = new FileOutputStream(files[6]);
        out.write(' ');
        out.close();
        content=cache.lookup(names[7]);
        assertEquals(70,cache.getCachedSize());
        assertEquals(1,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[6]);
        assertEquals(71,cache.getCachedSize());
        assertEquals(2,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[0]);
        assertEquals(72,cache.getCachedSize());
        assertEquals(3,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[1]);
        assertEquals(82,cache.getCachedSize());
        assertEquals(4,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[2]);
        assertEquals(32,cache.getCachedSize());
        assertEquals(4,cache.getCachedFiles());

        Thread.sleep(2);
        
        content=cache.lookup(names[3]);
        assertEquals(61,cache.getCachedSize());
        assertEquals(4,cache.getCachedFiles());

        Thread.sleep(2);
        
        cache.flushCache();
        assertEquals(0,cache.getCachedSize());
        assertEquals(0,cache.getCachedFiles());
    }
}
