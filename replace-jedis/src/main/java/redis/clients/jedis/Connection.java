package redis.clients.jedis;

/**
 * @author wangguangwu
 */
public class Connection {

    private final JedisSocketFactory socketFactory;


    public Connection(final String host, final int port) {
        this(new HostAndPort(host, port));
    }

    public Connection(final HostAndPort hostAndPort) {
        this(new DefaultJedisSocketFactory(hostAndPort));
    }


    public Connection(final JedisSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }
}
