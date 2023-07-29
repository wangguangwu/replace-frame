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

        // 验证连接，如果返回 PONG 表示连接成功
        System.out.println("Connection successful: " + jedis.ping());
    }
}
