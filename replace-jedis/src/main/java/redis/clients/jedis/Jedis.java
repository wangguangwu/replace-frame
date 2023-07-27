package redis.clients.jedis;

/**
 * @author wangguangwu
 */
public class Jedis {

    protected final Connection connection;

    public Jedis(final String host, final int port) {
        connection = new Connection(host, port);
    }

}
