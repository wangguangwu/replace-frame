package redis.clients.jedis;

/**
 * @author wangguangwu
 */
public class HostAndPort {

    private final String host;
    private final int port;

    public HostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
    }

}
