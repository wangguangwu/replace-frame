package redis.clients.jedis;

/**
 * @author wangguangwu
 */
public interface HostAndPortMapper {

    HostAndPort getHostAndPort(HostAndPort hap);

}
