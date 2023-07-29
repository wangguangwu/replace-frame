package redis.clients.jedis.util;

import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author wangguangwu
 */
public class RedisInputStream extends FilterInputStream {
    private static final int INPUT_BUFFER_SIZE = Integer.parseInt(
            System.getProperty("jedis.bufferSize.input",
                    System.getProperty("jedis.bufferSize", "8192")));

    protected final byte[] buf;

    protected int count, limit;

    public RedisInputStream(InputStream in) {
        this(in, INPUT_BUFFER_SIZE);
    }

    public RedisInputStream(InputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    public byte readByte() throws JedisConnectionException {
        ensureFill();
        // 返回已经读取的数据
        return buf[count++];
    }

    private void ensureFill() throws JedisConnectionException {
        // 如果读取的字节数超出了缓冲区的极限
        if (count >= limit) {
            try {
                // 读取新的数据
                limit = in.read(buf);
                // 重制已经读取的字节数
                count = 0;
                if (limit == -1) {
                    throw new JedisConnectionException("Unexpected end of stream.");
                }
            } catch (IOException e) {
                throw new JedisConnectionException(e);
            }
        }
    }

    public String readLine() {
        // 按行读取数据
        final StringBuilder sb = new StringBuilder();
        while (true) {
            ensureFill();

            byte b = buf[count++];
            if (b == '\r') {
                ensureFill(); // Must be one more byte

                byte c = buf[count++];
                if (c == '\n') {
                    break;
                }
                sb.append((char) b);
                sb.append((char) c);
            } else {
                sb.append((char) b);
            }
        }

        final String reply = sb.toString();
        if (reply.length() == 0) {
            throw new JedisConnectionException("It seems like server has closed the connection.");
        }

        return reply;
    }

    public byte[] readLineBytes() {

        /*
         * This operation should only require one fill. In that typical case we optimize allocation and
         * copy of the byte array. In the edge case where more than one fill is required then we take a
         * slower path and expand a byte array output stream as is necessary.
         */

        ensureFill();

        int pos = count;
        final byte[] buf = this.buf;
        while (true) {
            if (pos == limit) {
                return readLineBytesSlowly();
            }

            if (buf[pos++] == '\r') {
                if (pos == limit) {
                    return readLineBytesSlowly();
                }

                if (buf[pos++] == '\n') {
                    break;
                }
            }
        }

        final int N = (pos - count) - 2;
        final byte[] line = new byte[N];
        System.arraycopy(buf, count, line, 0, N);
        count = pos;
        return line;
    }

    public long readLongCrLf() {
        final byte[] buf = this.buf;

        ensureFill();

        final boolean isNeg = buf[count] == '-';
        if (isNeg) {
            ++count;
        }

        long value = 0;
        while (true) {
            ensureFill();

            final int b = buf[count++];
            if (b == '\r') {
                ensureFill();

                if (buf[count++] != '\n') {
                    throw new JedisConnectionException("Unexpected character!");
                }

                break;
            } else {
                value = value * 10 + b - '0';
            }
        }

        return (isNeg ? -value : value);
    }

    public int readIntCrLf() {
        return (int) readLongCrLf();
    }

    @Override
    public int read(byte[] b, int off, int len) throws JedisConnectionException {
        // 将缓冲区数据发送到服务器端
        ensureFill();

        // 读取数据
        final int length = Math.min(limit - count, len);
        System.arraycopy(buf, count, b, off, length);
        count += length;
        return length;
    }

    private byte[] readLineBytesSlowly() {
    ByteArrayOutputStream bout = null;
    while (true) {
      ensureFill();

      byte b = buf[count++];
      if (b == '\r') {
        ensureFill(); // Must be one more byte

        byte c = buf[count++];
        if (c == '\n') {
          break;
        }

        if (bout == null) {
          bout = new ByteArrayOutputStream(16);
        }

        bout.write(b);
        bout.write(c);
      } else {
        if (bout == null) {
          bout = new ByteArrayOutputStream(16);
        }

        bout.write(b);
      }
    }

    return bout == null ? new byte[0] : bout.toByteArray();
  }
}
