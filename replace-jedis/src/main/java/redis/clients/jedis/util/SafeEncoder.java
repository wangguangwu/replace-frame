package redis.clients.jedis.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author wangguangwu
 */
public class SafeEncoder {

    public static volatile Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static byte[] encode(final String str) {
        if (str == null) {
            throw new IllegalArgumentException("null value cannot be sent to redis");
        }
        return str.getBytes(DEFAULT_CHARSET);
    }

    public static String encode(final byte[] data) {
        return new String(data, DEFAULT_CHARSET);
    }

}
