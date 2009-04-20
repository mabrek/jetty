// ========================================================================
// Copyright (c) 2008-2009 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.security.authentication;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.log.Log;

/**
 * Deferred Authentictor
 * <p>
 * Authenticator that defers non manditory authentication by
 * returning a {@link Authentication.Deferred} instance that 
 * defers authentication until a call
 * to {@link Authentication.Deferred#authenticate()} or
 * {@link Authentication.Deferred#authenticate(ServletRequest, ServletResponse)}.
 * 
 * @version $Rev: 4793 $ $Date: 2009-03-19 00:00:01 +0100 (Thu, 19 Mar 2009) $
 */
public class DeferredAuthenticator extends DelegateAuthenticator
{
    /* ------------------------------------------------------------ */
    public DeferredAuthenticator(Authenticator delegate)
    {
        super(delegate);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.security.Authenticator#validateRequest(ServletRequest,
     *      ServletResponse, boolean)
     */
    public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory) throws ServerAuthException
    {
        if (!mandatory)
        {
            return new DeferredAuthentication(_delegate,request,response);
        }
        return _delegate.validateRequest(request,response,mandatory);
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static class DeferredAuthentication implements Authentication.Deferred
    {
        protected final Authenticator _authenticator;
        protected final ServletRequest _request;
        protected final ServletResponse _response;

        private Authentication _delegate;
        private IdentityService _identityService;

        public DeferredAuthentication(Authenticator authenticator, ServletRequest request, ServletResponse response)
        {
            if (authenticator == null)
                throw new NullPointerException("No Authenticator");
            this._authenticator = authenticator;
            this._request = request;
            this._response = response;
        }
        
        /* ------------------------------------------------------------ */
        /** Get the identityService.
         * @return the identityService
         */
        public IdentityService getIdentityService()
        {
            return _identityService;
        }

        /* ------------------------------------------------------------ */
        /** Set the identityService.
         * @param identityService the identityService to set
         */
        public void setIdentityService(IdentityService identityService)
        {
            _identityService = identityService;
        }

        /* ------------------------------------------------------------ */
        /**
         * @see org.eclipse.jetty.server.Authentication.Deferred#authenticate()
         */
        public Authentication authenticate()
        {
            try
            {
                Authentication authentication = _authenticator.validateRequest(_request,__nullResponse,false);
                
                if (authentication!=null && (authentication instanceof Authentication.User) && !(authentication instanceof Authentication.ResponseSent))
                {
                    if (_identityService!=null)
                        _identityService.associate(((Authentication.User)authentication).getUserIdentity());
                    return authentication;
                }
            }
            catch (ServerAuthException e)
            {
                Log.debug(e);
            }
            return Authentication.UNAUTHENTICATED;
        }
        
        /* ------------------------------------------------------------ */
        /**
         * @see org.eclipse.jetty.server.Authentication.Deferred#authenticate(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
         */
        public Authentication authenticate(ServletRequest request, ServletResponse response)
        {
            try
            {
                Authentication authentication = _authenticator.validateRequest(_request,response,true);
                if (authentication instanceof Authentication.User && _identityService!=null)
                    _identityService.associate(((Authentication.User)authentication).getUserIdentity());
                return authentication;
            }
            catch (ServerAuthException e)
            {
                Log.debug(e);
            }
            return Authentication.UNAUTHENTICATED;
        }

        /* ------------------------------------------------------------ */
        /**
         * @see org.eclipse.jetty.server.Authentication.Deferred#login(java.lang.String, java.lang.String)
         */
        public Authentication login(String username, String password)
        {
            return null; // TODO implement
        }

    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static HttpServletResponse __nullResponse = new HttpServletResponse()
    {
        public void addCookie(Cookie cookie)
        {
        }

        public void addDateHeader(String name, long date)
        {
        }

        public void addHeader(String name, String value)
        {
        }

        public void addIntHeader(String name, int value)
        {
        }

        public boolean containsHeader(String name)
        {
            return false;
        }

        public String encodeRedirectURL(String url)
        {
            return null;
        }

        public String encodeRedirectUrl(String url)
        {
            return null;
        }

        public String encodeURL(String url)
        {
            return null;
        }

        public String encodeUrl(String url)
        {
            return null;
        }

        public void sendError(int sc) throws IOException
        {
        }

        public void sendError(int sc, String msg) throws IOException
        {
        }

        public void sendRedirect(String location) throws IOException
        {
        }

        public void setDateHeader(String name, long date)
        {
        }

        public void setHeader(String name, String value)
        {
        }

        public void setIntHeader(String name, int value)
        {
        }

        public void setStatus(int sc)
        {
        }

        public void setStatus(int sc, String sm)
        {
        }

        public void flushBuffer() throws IOException
        {
        }

        public int getBufferSize()
        {
            return 1024;
        }

        public String getCharacterEncoding()
        {
            return null;
        }

        public String getContentType()
        {
            return null;
        }

        public Locale getLocale()
        {
            return null;
        }

        public ServletOutputStream getOutputStream() throws IOException
        {
            return __nullOut;
        }

        public PrintWriter getWriter() throws IOException
        {
            return IO.getNullPrintWriter();
        }

        public boolean isCommitted()
        {
            return true;
        }

        public void reset()
        {
        }

        public void resetBuffer()
        {
        }

        public void setBufferSize(int size)
        {
        }

        public void setCharacterEncoding(String charset)
        {
        }

        public void setContentLength(int len)
        {
        }

        public void setContentType(String type)
        {
        }

        public void setLocale(Locale loc)
        {
        }

    };

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static ServletOutputStream __nullOut = new ServletOutputStream()
    {
        public void write(int b) throws IOException
        {
        }

        public void print(String s) throws IOException
        {
        }

        public void println(String s) throws IOException
        {
        }
    };
}