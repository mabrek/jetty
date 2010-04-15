package org.eclipse.jetty.server.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.http.HttpParser;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ConnectedEndPoint;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.nio.IndirectNIOBuffer;
import org.eclipse.jetty.io.nio.SelectChannelEndPoint;
import org.eclipse.jetty.io.nio.SelectorManager;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * <p>Implementation of a tunnelling proxy that supports HTTP CONNECT and transparent proxy.</p>
 * <p>To work as CONNECT proxy, objects of this class must be instantiated using the no-arguments
 * constructor, since the remote server information will be present in the CONNECT URI.</p>
 * <p>To work as transparent proxy, objects of this class must be instantiated using the string
 * argument constructor, passing the remote host address and port in the form {@code host:port}.</p>
 *
 * @version $Revision$ $Date$
 */
public class ProxyHandler extends AbstractHandler
{
    private final Logger _logger = Log.getLogger(getClass().getName());
    private final SelectorManager _selectorManager = new Manager();
    private final String _serverAddress;
    private volatile int _connectTimeout = 5000;
    private volatile int _writeTimeout = 30000;
    private volatile ThreadPool _threadPool;
    private volatile ThreadPool _privateThreadPool;

    /**
     * <p>Constructor to be used to make this proxy work via HTTP CONNECT.</p>
     */
    public ProxyHandler()
    {
        this(null);
    }

    /**
     * <p>Constructor to be used to make this proxy work as transparent proxy.</p>
     *
     * @param serverAddress the address of the remote server in the form {@code host:port}
     */
    public ProxyHandler(String serverAddress)
    {
        _serverAddress = serverAddress;
    }

    /**
     * @return the timeout, in milliseconds, to connect to the remote server
     */
    public int getConnectTimeout()
    {
        return _connectTimeout;
    }

    /**
     * @param connectTimeout the timeout, in milliseconds, to connect to the remote server
     */
    public void setConnectTimeout(int connectTimeout)
    {
        _connectTimeout = connectTimeout;
    }

    /**
     * @return the timeout, in milliseconds, to write data to a peer
     */
    public int getWriteTimeout()
    {
        return _writeTimeout;
    }

    /**
     * @param writeTimeout the timeout, in milliseconds, to write data to a peer
     */
    public void setWriteTimeout(int writeTimeout)
    {
        _writeTimeout = writeTimeout;
    }

    @Override
    public void setServer(Server server)
    {
        super.setServer(server);

        server.getContainer().update(this,null,_selectorManager,"selectManager");

        if (_privateThreadPool!=null)
            server.getContainer().update(this,null,_privateThreadPool,"threadpool",true);
        else
            _threadPool=server.getThreadPool();
    }

    /**
     * @return the thread pool
     */
    public ThreadPool getThreadPool()
    {
        return _threadPool;
    }

    /**
     * @param the thread pool
     */
    public void setThreadPool(ThreadPool threadPool)
    {
        if (getServer()!=null)
            getServer().getContainer().update(this,_privateThreadPool,threadPool,"threadpool",true);
        _threadPool=_privateThreadPool=threadPool;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        if (_threadPool==null)
            _threadPool=getServer().getThreadPool();
        if (_threadPool instanceof LifeCycle && !((LifeCycle)_threadPool).isRunning())
            ((LifeCycle)_threadPool).start();

        _selectorManager.start();
        _threadPool.dispatch(new Runnable()
        {
            public void run()
            {
                while (isRunning())
                {
                    try
                    {
                        _selectorManager.doSelect(0);
                    }
                    catch (IOException x)
                    {
                        _logger.warn("Unexpected exception", x);
                    }
                }
            }
        });
    }

    @Override
    protected void doStop() throws Exception
    {
        _selectorManager.stop();

        ThreadPool threadPool = _threadPool;
        if (threadPool != null && threadPool instanceof LifeCycle)
            ((LifeCycle)threadPool).stop();

        super.doStop();
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (HttpMethods.CONNECT.equalsIgnoreCase(request.getMethod()))
        {
            _logger.debug("CONNECT request for {}", request.getRequestURI(), null);
            handle(request, response, request.getRequestURI());
        }
        else
        {
            _logger.debug("Plain request for {}", _serverAddress, null);
            if (_serverAddress == null)
                throw new ServletException("Parameter 'serverAddress' cannot be null");
            handle(request, response, _serverAddress);
        }
    }

    /**
     * <p>Handles a tunnelling request, either a CONNECT or a transparent request.</p>
     * <p>CONNECT requests may have authentication headers such as <code>Proxy-Authorization</code>
     * that authenticate the client with the proxy.</p>
     *
     * @param request the http request
     * @param response the http response
     * @param serverAddress the remote server address in the form {@code host:port}
     * @throws ServletException if an application error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void handle(HttpServletRequest request, HttpServletResponse response, String serverAddress) throws ServletException, IOException
    {
        boolean proceed = handleAuthentication(request, response, serverAddress);
        if (!proceed)
            return;

        String host = serverAddress;
        int port = 80;
        boolean secure = false;
        int colon = serverAddress.indexOf(':');
        if (colon > 0)
        {
            host = serverAddress.substring(0, colon);
            port = Integer.parseInt(serverAddress.substring(colon + 1));
            secure = isTunnelSecure(host, port);
        }

        setupTunnel(request, response, host, port, secure);
    }

    /**
     * <p>Returns whether the given {@code host} and {@code port} identify a SSL communication channel.<p>
     * <p>Default implementation returns true if the {@code port} is 443.</p>
     *
     * @param host the host to connect to
     * @param port the port to connect to
     * @return true if the communication channel is confidential, false otherwise
     */
    protected boolean isTunnelSecure(String host, int port)
    {
        return port == 443;
    }

    /**
     * <p>Handles the authentication before setting up the tunnel to the remote server.</p>
     * <p>The default implementation returns true.</p>
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param address the address of the remote server in the form {@code host:port}.
     * @return true to allow to connect to the remote host, false otherwise
     * @throws ServletException to report a server error to the caller
     * @throws IOException to report a server error to the caller
     */
    protected boolean handleAuthentication(HttpServletRequest request, HttpServletResponse response, String address) throws ServletException, IOException
    {
        return true;
    }

    protected void setupTunnel(HttpServletRequest request, HttpServletResponse response, String host, int port, boolean secure) throws IOException
    {
        SocketChannel channel = connect(request, host, port);
        channel.configureBlocking(false);

        // Transfer unread data from old connection to new connection
        // We need to copy the data to avoid races:
        // 1. when this unread data is written and the server replies before the clientToProxy
        // connection is installed (it is only installed after returning from this method)
        // 2. when the client sends data before this unread data has been written.
        HttpConnection httpConnection = HttpConnection.getCurrentConnection();
        Buffer headerBuffer = ((HttpParser)httpConnection.getParser()).getHeaderBuffer();
        Buffer bodyBuffer = ((HttpParser)httpConnection.getParser()).getBodyBuffer();
        int length = headerBuffer == null ? 0 : headerBuffer.length();
        length += bodyBuffer == null ? 0 : bodyBuffer.length();
        IndirectNIOBuffer buffer = null;
        if (length > 0)
        {
            buffer = new IndirectNIOBuffer(length);
            if (headerBuffer != null)
            {
                buffer.put(headerBuffer);
                headerBuffer.clear();
            }
            if (bodyBuffer != null)
            {
                buffer.put(bodyBuffer);
                bodyBuffer.clear();
            }
        }

        // Setup connections, before registering the channel to avoid races
        // where the server sends data before the connections are set up
        ProxyToServerConnection proxyToServer = newProxyToServerConnection(secure, buffer);
        ClientToProxyConnection clientToProxy = newClientToProxyConnection(channel, httpConnection.getEndPoint(), httpConnection.getTimeStamp());
        clientToProxy.setConnection(proxyToServer);
        proxyToServer.setConnection(clientToProxy);

        upgradeConnection(request, response, clientToProxy);
    }

    protected ClientToProxyConnection newClientToProxyConnection(SocketChannel channel, EndPoint endPoint, long timeStamp)
    {
        return new ClientToProxyConnection(channel, endPoint, timeStamp);
    }

    protected ProxyToServerConnection newProxyToServerConnection(boolean secure, IndirectNIOBuffer buffer)
    {
        return new ProxyToServerConnection(secure, buffer);
    }

    /**
     * <p>Establishes a connection to the remote server.</p>
     * @param request the HTTP request that initiated the tunnel
     * @param host the host to connect to
     * @param port the port to connect to
     * @return a {@link SocketChannel} connected to the remote server
     * @throws IOException if the connection cannot be established
     */
    protected SocketChannel connect(HttpServletRequest request, String host, int port) throws IOException
    {
        _logger.debug("Establishing connection to {}:{}", host, port);
        // Connect to remote server
        SocketChannel channel = SocketChannel.open();
        channel.socket().setTcpNoDelay(true);
        channel.socket().connect(new InetSocketAddress(host, port), getConnectTimeout());
        _logger.debug("Established connection to {}:{}", host, port);
        return channel;
    }

    private void upgradeConnection(HttpServletRequest request, HttpServletResponse response, Connection connection) throws IOException
    {
        // CONNECT expects a 200 response
        response.setStatus(HttpServletResponse.SC_OK);
        // Flush it so that the client receives it
        response.flushBuffer();
        // Set the new connection as request attribute and change the status to 101
        // so that Jetty understands that it has to upgrade the connection
        request.setAttribute("org.eclipse.jetty.io.Connection", connection);
        response.setStatus(HttpServletResponse.SC_SWITCHING_PROTOCOLS);
        _logger.debug("Upgraded connection to {}", connection, null);
    }

    private void register(SocketChannel channel, ProxyToServerConnection proxyToServer) throws IOException
    {
        _selectorManager.register(channel, proxyToServer);
        proxyToServer.waitReady(_connectTimeout);
    }

    /**
     * <p>Reads (with non-blocking semantic) into the given {@code buffer} from the given {@code endPoint}.</p>
     * @param endPoint the endPoint to read from
     * @param buffer the buffer to read data into
     * @return the number of bytes read (possibly 0 since the read is non-blocking)
     * or -1 if the channel has been closed remotely
     * @throws IOException if the endPoint cannot be read
     */
    protected int read(EndPoint endPoint, Buffer buffer) throws IOException
    {
        return endPoint.fill(buffer);
    }

    /**
     * <p>Writes (with blocking semantic) the given buffer of data onto the given endPoint.</p>
     *
     * @param endPoint the endPoint to write to
     * @param buffer the buffer to write
     * @throws IOException if the buffer cannot be written
     */
    protected void write(EndPoint endPoint, Buffer buffer) throws IOException
    {
        if (buffer == null)
            return;

        int length = buffer.length();
        StringBuilder builder = new StringBuilder();
        int written = endPoint.flush(buffer);
        builder.append(written);
        buffer.compact();
        if (!endPoint.isBlocking())
        {
            while (buffer.space() == 0)
            {
                boolean ready = endPoint.blockWritable(getWriteTimeout());
                if (!ready)
                    throw new IOException("Write timeout");

                written = endPoint.flush(buffer);
                builder.append("+").append(written);
                buffer.compact();
            }
        }
        _logger.debug("Written {}/{} bytes " + endPoint, builder, length);
    }

    private class Manager extends SelectorManager
    {
        @Override
        protected SocketChannel acceptChannel(SelectionKey key) throws IOException
        {
            // This is a client-side selector manager
            throw new IllegalStateException();
        }

        @Override
        protected SelectChannelEndPoint newEndPoint(SocketChannel channel, SelectSet selectSet, SelectionKey selectionKey) throws IOException
        {
            ProxyToServerConnection proxyToServer = (ProxyToServerConnection)selectionKey.attachment();
            if (proxyToServer._secure)
            {
                throw new UnsupportedOperationException();
//                return new SslSelectChannelEndPoint(???, channel, selectSet, selectionKey, sslContext.createSSLEngine(address.host, address.port));
            }
            else
            {
                return new SelectChannelEndPoint(channel, selectSet, selectionKey);
            }
        }

        @Override
        protected Connection newConnection(SocketChannel channel, SelectChannelEndPoint endpoint)
        {
            ProxyToServerConnection proxyToServer = (ProxyToServerConnection)endpoint.getSelectionKey().attachment();
            proxyToServer.setTimeStamp(System.currentTimeMillis());
            proxyToServer.setEndPoint(endpoint);
            return proxyToServer;
        }

        @Override
        protected void endPointOpened(SelectChannelEndPoint endpoint)
        {
            ProxyToServerConnection proxyToServer = (ProxyToServerConnection)endpoint.getSelectionKey().attachment();
            proxyToServer.ready();
        }

        @Override
        public boolean dispatch(Runnable task)
        {
            return _threadPool.dispatch(task);
        }

        @Override
        protected void endPointClosed(SelectChannelEndPoint endpoint)
        {
        }

        @Override
        protected void endPointUpgraded(ConnectedEndPoint endpoint, Connection oldConnection)
        {
        }
    }

    public class ProxyToServerConnection implements Connection
    {
        private final CountDownLatch _ready = new CountDownLatch(1);
        private final Buffer _buffer = new IndirectNIOBuffer(1024);
        private final boolean _secure;
        private volatile Buffer _data;
        private volatile ClientToProxyConnection _toClient;
        private volatile long _timestamp;
        private volatile SelectChannelEndPoint _endPoint;

        public ProxyToServerConnection(boolean secure, Buffer data)
        {
            _secure = secure;
            _data = data;
        }

        public Connection handle() throws IOException
        {
            _logger.debug("ProxyToServer: handle entered");
            if (_data != null)
            {
                write(_endPoint, _data);
                _data = null;
            }

            while (true)
            {
                int read = read(_endPoint, _buffer);

                if (read == -1)
                {
                    _logger.debug("ProxyToServer: closed connection {}", _endPoint, null);
                    _toClient.close();
                    break;
                }

                if (read == 0)
                    break;

                _logger.debug("ProxyToServer: read {} bytes {}", read, _endPoint);
                write(_toClient._endPoint, _buffer);
            }
            _logger.debug("ProxyToServer: handle exited");
            return this;
        }

        public void setConnection(ClientToProxyConnection connection)
        {
            _toClient = connection;
        }

        public long getTimeStamp()
        {
            return _timestamp;
        }

        public void setTimeStamp(long timestamp)
        {
            _timestamp = timestamp;
        }

        public void setEndPoint(SelectChannelEndPoint endpoint)
        {
            _endPoint = endpoint;
        }

        public boolean isIdle()
        {
            return false;
        }

        public boolean isSuspended()
        {
            return false;
        }

        public void ready()
        {
            _ready.countDown();
        }

        public void waitReady(long timeout) throws IOException
        {
            try
            {
                _ready.await(timeout, TimeUnit.MILLISECONDS);
            }
            catch (final InterruptedException x)
            {
                throw new IOException(){{initCause(x);}};
            }
        }

        public void close() throws IOException
        {
            _endPoint.close();
        }
    }

    public class ClientToProxyConnection implements Connection
    {
        private final Buffer _buffer = new IndirectNIOBuffer(1024);
        private final SocketChannel _channel;
        private final EndPoint _endPoint;
        private final long _timestamp;
        private volatile ProxyToServerConnection _toServer;
        private boolean _firstTime = true;

        public ClientToProxyConnection(SocketChannel channel, EndPoint endPoint, long timestamp)
        {
            _channel = channel;
            _endPoint = endPoint;
            _timestamp = timestamp;
        }

        public Connection handle() throws IOException
        {
            _logger.debug("ClientToProxy: handle entered");

            if (_firstTime)
            {
                _firstTime = false;
                register(_channel, _toServer);
            }

            while (true)
            {
                int read = read(_endPoint, _buffer);

                if (read == -1)
                {
                    _logger.debug("ClientToProxy: closed connection {}", _endPoint, null);
                    _toServer.close();
                    break;
                }

                if (read == 0)
                    break;

                _logger.debug("ClientToProxy: read {} bytes {}", read, _endPoint);
                write(_toServer._endPoint, _buffer);
            }
            _logger.debug("ClientToProxy: handle exited");
            return this;
        }

        public long getTimeStamp()
        {
            return _timestamp;
        }

        public boolean isIdle()
        {
            return false;
        }

        public boolean isSuspended()
        {
            return false;
        }

        public void setConnection(ProxyToServerConnection connection)
        {
            _toServer = connection;
        }

        public void close() throws IOException
        {
            _endPoint.close();
        }
    }
}
