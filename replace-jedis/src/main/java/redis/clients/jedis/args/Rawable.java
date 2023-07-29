package redis.clients.jedis.args;

/**
 * @author wangguangwu
 */
public interface Rawable {

    /**
     * Get byte array.
     *
     * @return binary
     */
    byte[] getRaw();

}
