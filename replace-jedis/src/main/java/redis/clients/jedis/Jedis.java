package redis.clients.jedis;

/**
 * @author wangguangwu
 */
public class Jedis implements ServerCommands {

    protected final Connection connection;

    public Jedis(final String host, final int port) {
        connection = new Connection(host, port);
    }

    @Override
    public String ping() {
        checkIsInMultiOrPipeline();
        // 发送命令
        connection.sendCommand(Protocol.Command.PING);
        return connection.getStatusCodeReply();
    }

    protected void checkIsInMultiOrPipeline() {
        // 判断这个 Jedis 连接是否处于事务或者管道状态
        // 这两种状态下不支持执行普康 Jedis 操作
//        if (transaction != null) {
//            throw new IllegalStateException(
//                    "Cannot use Jedis when in Multi. Please use Transaction or reset jedis state.");
//        } else if (pipeline != null && pipeline.hasPipelinedResponse()) {
//            throw new IllegalStateException(
//                    "Cannot use Jedis when in Pipeline. Please use Pipeline or reset jedis state.");
//        }
    }
}
