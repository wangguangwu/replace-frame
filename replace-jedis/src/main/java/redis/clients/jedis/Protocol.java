package redis.clients.jedis;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangguangwu
 */
public class Protocol {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 6379;

    public static final int DEFAULT_TIMEOUT = 2000;

    public static final byte ASTERISK_BYTE = '*';
    public static final byte DOLLAR_BYTE = '$';
    public static final byte MINUS_BYTE = '-';
    public static final byte PLUS_BYTE = '+';
    public static final byte COLON_BYTE = ':';

    private static final String ASK_PREFIX = "ASK ";
    private static final String MOVED_PREFIX = "MOVED ";
    private static final String CLUSTERDOWN_PREFIX = "CLUSTERDOWN ";
    private static final String BUSY_PREFIX = "BUSY ";
    private static final String NOSCRIPT_PREFIX = "NOSCRIPT ";
    private static final String WRONGPASS_PREFIX = "WRONGPASS";
    private static final String NOPERM_PREFIX = "NOPERM";


    public static enum Command implements ProtocolCommand {
        PING;

        private final byte[] raw;

        private Command() {
            this.raw = SafeEncoder.encode(name());
        }

        @Override
        public byte[] getRaw() {
            return raw;
        }
    }

    public static void sendCommand(final RedisOutputStream os, CommandArguments args) {
        try {
            // ASTERISK_BYTE (*) 符号
            // 在 Redis 协议中，这个符号用于表示接下来要读取的参数数量。
            // 比如说，*3 表示接下来会有 3 个参数
            // 将参数的个数写入流中
            os.write(ASTERISK_BYTE);
            os.writeIntCrLf(args.size());
            for (Rawable arg : args) {
                // DOLLAR_BYTE ($) 符号
                // 在 Redis 协议中，这个符号用于表示接下来要读取的参数的字节长度。
                // 比如说，$3 表示接下来的参数是一个由 3 个字节组成的数据。
                os.write(DOLLAR_BYTE);
                final byte[] bin = arg.getRaw();
                os.writeIntCrLf(bin.length);
                os.write(bin);
                // 写个换行符
                os.writeCrLf();
            }
        } catch (IOException e) {
            throw new JedisConnectionException(e);
        }
    }

    public static String readErrorLineIfPossible(RedisInputStream is) {
        final byte b = is.readByte();
        // if buffer contains other type of response, just ignore.
        // '-' 表述服务器返回的是一个错误消息
        if (b != MINUS_BYTE) {
            return null;
        }
        return is.readLine();
    }

    public static Object read(final RedisInputStream is) {
        return process(is);
    }

    private static Object process(final RedisInputStream is) {
        // 按字节进行读取
        final byte b = is.readByte();
        switch (b) {
            // 表示服务器返回的是一个状态回复（status reply）。这是 Redis 服务器的一个正常响应，通常表示命令执行成功。
            case PLUS_BYTE:
                return processStatusCodeReply(is);
            // 表示服务器返回的是一个批量回复（bulk reply）。
            case DOLLAR_BYTE:
                return processBulkReply(is);
            // 表示服务器返回的是一个多批量回复（multi bulk reply）。
            case ASTERISK_BYTE:
                return processMultiBulkReply(is);
            // 表示服务器返回的是一个整数回复（integer reply）。
            case COLON_BYTE:
                return processInteger(is);
            // 表示服务器返回的是一个错误消息（error message）。
            case MINUS_BYTE:
                processError(is);
                return null;
            default:
                throw new JedisConnectionException("Unknown reply: " + (char) b);
        }
    }

    private static byte[] processStatusCodeReply(final RedisInputStream is) {
        return is.readLineBytes();
    }

    private static byte[] processBulkReply(final RedisInputStream is) {
        final int len = is.readIntCrLf();
        if (len == -1) {
            return null;
        }

        final byte[] read = new byte[len];
        int offset = 0;
        while (offset < len) {
            final int size = is.read(read, offset, (len - offset));
            if (size == -1) {
                throw new JedisConnectionException("It seems like server has closed the connection.");
            }
            offset += size;
        }

        // read 2 more bytes for the command delimiter
        is.readByte();
        is.readByte();

        return read;
    }

    private static List<Object> processMultiBulkReply(final RedisInputStream is) {
        final int num = is.readIntCrLf();
        if (num == -1) {
            return null;
        }
        final List<Object> ret = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            try {
                ret.add(process(is));
            } catch (JedisDataException e) {
                ret.add(e);
            }
        }
        return ret;
    }

    private static Long processInteger(final RedisInputStream is) {
        return is.readLongCrLf();
    }

    private static void processError(final RedisInputStream is) {
        String message = is.readLine();
        // TODO: I'm not sure if this is the best way to do this.
        // Maybe Read only first 5 bytes instead?
        if (message.startsWith(MOVED_PREFIX)) {
            String[] movedInfo = parseTargetHostAndSlot(message);
//      throw new JedisMovedDataException(message, new HostAndPort(movedInfo[1],
//          Integer.parseInt(movedInfo[2])), Integer.parseInt(movedInfo[0]));
            throw new JedisMovedDataException(message, HostAndPort.from(movedInfo[1]), Integer.parseInt(movedInfo[0]));
        } else if (message.startsWith(ASK_PREFIX)) {
            String[] askInfo = parseTargetHostAndSlot(message);
//      throw new JedisAskDataException(message, new HostAndPort(askInfo[1],
//          Integer.parseInt(askInfo[2])), Integer.parseInt(askInfo[0]));
            throw new JedisAskDataException(message, HostAndPort.from(askInfo[1]), Integer.parseInt(askInfo[0]));
        } else if (message.startsWith(CLUSTERDOWN_PREFIX)) {
            throw new UnsupportedOperationException(message);
        } else if (message.startsWith(BUSY_PREFIX)) {
            throw new UnsupportedOperationException(message);
        } else if (message.startsWith(NOSCRIPT_PREFIX)) {
            throw new UnsupportedOperationException(message);
        } else if (message.startsWith(WRONGPASS_PREFIX)) {
            throw new UnsupportedOperationException(message);
        } else if (message.startsWith(NOPERM_PREFIX)) {
            throw new UnsupportedOperationException(message);
        }
        throw new JedisDataException(message);
    }

    private static String[] parseTargetHostAndSlot(String clusterRedirectResponse) {
        String[] response = new String[2];
        String[] messageInfo = clusterRedirectResponse.split(" ");
        response[0] = messageInfo[1];
        response[1] = messageInfo[2];
        return response;
    }
}
