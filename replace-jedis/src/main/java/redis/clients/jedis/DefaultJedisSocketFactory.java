package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.Socket;

/**
 * @author wangguangwu
 */

public class DefaultJedisSocketFactory implements JedisSocketFactory {

    protected static final HostAndPort DEFAULT_HOST_AND_PORT = new HostAndPort(Protocol.DEFAULT_HOST,
            Protocol.DEFAULT_PORT);

    private volatile HostAndPort hostAndPort = DEFAULT_HOST_AND_PORT;

    public Socket createSocket() throws JedisConnectionException {
        return null;
    }

    public DefaultJedisSocketFactory(HostAndPort hostAndPort) {
        this(hostAndPort, null);
    }

    public DefaultJedisSocketFactory(HostAndPort hostAndPort, JedisClientConfig config) {
        if (hostAndPort != null) {
            this.hostAndPort = hostAndPort;
        }
        if (config != null) {
//            this.connectionTimeout = config.getConnectionTimeoutMillis();
//            this.socketTimeout = config.getSocketTimeoutMillis();
//            this.ssl = config.isSsl();
//            this.sslSocketFactory = config.getSslSocketFactory();
//            this.sslParameters = config.getSslParameters();
//            this.hostnameVerifier = config.getHostnameVerifier();
//            this.hostAndPortMapper = config.getHostAndPortMapper();
        }
    }
}
