package com.wangguangwu;

import org.junit.Test;
import redis.clients.jedis.Jedis;

/**
 * @author wangguangwu
 */
public class JedisTest {

    @Test
    public void testConnect() {
        Jedis jedis = new Jedis("127.0.0.1", 6379);
        System.out.println();
    }
}
