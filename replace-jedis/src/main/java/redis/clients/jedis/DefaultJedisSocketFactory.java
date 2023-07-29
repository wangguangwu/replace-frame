package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.IOUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author wangguangwu
 */

public class DefaultJedisSocketFactory implements JedisSocketFactory {

    protected static final HostAndPort DEFAULT_HOST_AND_PORT = new HostAndPort(Protocol.DEFAULT_HOST,
            Protocol.DEFAULT_PORT);

    private volatile HostAndPort hostAndPort = DEFAULT_HOST_AND_PORT;
    private HostAndPortMapper hostAndPortMapper = null;
    private int socketTimeout = Protocol.DEFAULT_TIMEOUT;
    private boolean ssl = false;
    private SSLSocketFactory sslSocketFactory = null;
    private SSLParameters sslParameters = null;
    private HostnameVerifier hostnameVerifier = null;
    private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;

    @Override
    public Socket createSocket() throws JedisConnectionException {
        Socket socket = null;
        try {
            // 获取连接的 host 和 port
            HostAndPort _hostAndPort = getSocketHostAndPort();
            // 连接配置的第一个主机
            socket = connectToFirstSuccessfulHost(_hostAndPort);
            // 设置超时时间
            socket.setSoTimeout(socketTimeout);

            // ssl 处理
            if (ssl) {
                SSLSocketFactory _sslSocketFactory = this.sslSocketFactory;
                if (null == _sslSocketFactory) {
                    _sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                }
                socket = _sslSocketFactory.createSocket(socket, _hostAndPort.getHost(), _hostAndPort.getPort(), true);

                if (null != sslParameters) {
                    ((SSLSocket) socket).setSSLParameters(sslParameters);
                }

                if (null != hostnameVerifier
                        && !hostnameVerifier.verify(_hostAndPort.getHost(), ((SSLSocket) socket).getSession())) {
                    String message = String.format(
                            "The connection to '%s' failed ssl/tls hostname verification.", _hostAndPort.getHost());
                    throw new JedisConnectionException(message);
                }
            }

            return socket;

        } catch (Exception ex) {
            // 关闭 sock 连接
            IOUtils.closeQuietly(socket);
            if (ex instanceof JedisConnectionException) {
                throw (JedisConnectionException) ex;
            } else {
                throw new JedisConnectionException("Failed to create socket.", ex);
            }
        }
    }

    public DefaultJedisSocketFactory(HostAndPort hostAndPort) {
        this(hostAndPort, null);
    }

    public DefaultJedisSocketFactory(HostAndPort hostAndPort, JedisClientConfig config) {
        if (hostAndPort != null) {
            this.hostAndPort = hostAndPort;
        }
        if (config != null) {
            // 当前方法传入的 config 为空，也就是不支持 ssl 模块，暂时跳过
//            this.connectionTimeout = config.getConnectionTimeoutMillis();
//            this.socketTimeout = config.getSocketTimeoutMillis();
//            this.ssl = config.isSsl();
//            this.sslSocketFactory = config.getSslSocketFactory();
//            this.sslParameters = config.getSslParameters();
//            this.hostnameVerifier = config.getHostnameVerifier();
//            this.hostAndPortMapper = config.getHostAndPortMapper();
        }
    }

    protected HostAndPort getSocketHostAndPort() {
        HostAndPortMapper mapper = hostAndPortMapper;
        HostAndPort hap = this.hostAndPort;
        if (mapper != null) {
            HostAndPort mapped = mapper.getHostAndPort(hap);
            if (mapped != null) {
                return mapped;
            }
        }
        return hap;
    }

    private Socket connectToFirstSuccessfulHost(HostAndPort hostAndPort) throws Exception {
        List<InetAddress> hosts = Arrays.asList(InetAddress.getAllByName(hostAndPort.getHost()));
        if (hosts.size() > 1) {
            Collections.shuffle(hosts);
        }

        JedisConnectionException jce = new JedisConnectionException("Failed to connect to any host resolved for DNS name.");
        for (InetAddress host : hosts) {
            try {
                Socket socket = new Socket();

                socket.setReuseAddress(true);
                socket.setKeepAlive(true); // Will monitor the TCP connection is valid
                socket.setTcpNoDelay(true); // Socket buffer Whetherclosed, to ensure timely delivery of data
                socket.setSoLinger(true, 0); // Control calls close () method, the underlying socket is closed immediately

                socket.connect(new InetSocketAddress(host.getHostAddress(), hostAndPort.getPort()), connectionTimeout);
                return socket;
            } catch (Exception e) {
                jce.addSuppressed(e);
            }
        }
        throw jce;
    }
}
