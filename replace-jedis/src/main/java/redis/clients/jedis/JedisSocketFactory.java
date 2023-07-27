package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.Socket;

/**
 * @author wangguangwu
 */
public interface JedisSocketFactory {

    Socket createSocket() throws JedisConnectionException;

}
