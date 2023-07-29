package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.IOUtils;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.net.Socket;

/**
 * @author wangguangwu
 */
public class Connection {

    private Socket socket;
    private final JedisSocketFactory socketFactory;
    private int soTimeout = 0;
    private RedisOutputStream outputStream;
    private RedisInputStream inputStream;
    private boolean broken = false;


    public Connection(final String host, final int port) {
        this(new HostAndPort(host, port));
    }

    public Connection(final HostAndPort hostAndPort) {
        this(new DefaultJedisSocketFactory(hostAndPort));
    }


    public Connection(final JedisSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    public void sendCommand(final ProtocolCommand cmd) {
        sendCommand(new CommandArguments(cmd));
    }

    public void sendCommand(final CommandArguments args) {
        try {
            // 连接 redis
            connect();
            // 把命令写入输出流中
            Protocol.sendCommand(outputStream, args);
        } catch (JedisConnectionException ex) {
            /*
             * When client send request which formed by invalid protocol, Redis send back error message
             * before close connection. We try to read it to provide reason of failure.
             */
            try {
                String errorMessage = Protocol.readErrorLineIfPossible(inputStream);
                if (errorMessage != null && errorMessage.length() > 0) {
                    ex = new JedisConnectionException(errorMessage, ex.getCause());
                }
            } catch (Exception e) {
                /*
                 * Catch any IOException or JedisConnectionException occurred from InputStream#read and just
                 * ignore. This approach is safe because reading error message is optional and connection
                 * will eventually be closed.
                 */
            }
            // Any other exceptions related to connection?
            // 设置标注为为断开
            broken = true;
            throw ex;
        }
    }

    public void connect() throws JedisConnectionException {
        if (!isConnected()) {
            try {
                socket = socketFactory.createSocket();
                soTimeout = socket.getSoTimeout(); //?

                // 输出、输入流
                outputStream = new RedisOutputStream(socket.getOutputStream());
                inputStream = new RedisInputStream(socket.getInputStream());

                // 成功初始化后，设置标志位为 false
                broken = false;

            } catch (JedisConnectionException jce) {
                // 设置标志位为 false
                setBroken();
                throw jce;

            } catch (IOException ioe) {
                // 设置标志位为 false
                setBroken();
                throw new JedisConnectionException("Failed to create input/output stream", ioe);

            } finally {
                // 如果标志位为 false，断开 sokcet
                if (broken) {
                    IOUtils.closeQuietly(socket);
                }
            }
        }
    }


    public void setBroken() {
        broken = true;
    }

    public boolean isConnected() {
        return socket != null && socket.isBound() && !socket.isClosed() && socket.isConnected()
                && !socket.isInputShutdown() && !socket.isOutputShutdown();
    }

    public String getStatusCodeReply() {
        // 刷新缓冲区
        flush();
        // 从服务器读取并返回一个响应
        final byte[] resp = (byte[]) readProtocolWithCheckingBroken();
        if (null == resp) {
            return null;
        } else {
            // 字节数组转字符串
            return SafeEncoder.encode(resp);
        }
    }

    protected void flush() {
        try {
            // 刷新缓冲区，将所有数据都发送到 redis 服务器
            outputStream.flush();
        } catch (IOException ex) {
            // 断开
            broken = true;
            throw new JedisConnectionException(ex);
        }
    }

    protected Object readProtocolWithCheckingBroken() {
        // 判断连接是否断开
        if (broken) {
            throw new JedisConnectionException("Attempting to read from a broken connection");
        }

        try {
            return Protocol.read(inputStream);
        } catch (JedisConnectionException exc) {
            broken = true;
            throw exc;
        }
    }
}
