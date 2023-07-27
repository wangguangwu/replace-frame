package redis.clients.jedis.exceptions;

/**
 * @author wangguangwu
 */
public class JedisConnectionException extends JedisException {

    public JedisConnectionException(String message) {
        super(message);
    }

    public JedisConnectionException(Throwable cause) {
        super(cause);
    }

    public JedisConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
