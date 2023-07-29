package redis.clients.jedis.util;

import java.io.*;

/**
 * @author wangguangwu
 */
public class RedisOutputStream extends FilterOutputStream {

    private static final int OUTPUT_BUFFER_SIZE = Integer.parseInt(
            System.getProperty("jedis.bufferSize.output",
                    System.getProperty("jedis.bufferSize", "8192")));

    protected final byte[] buf;

    protected int count;

    private final static int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999,
            999999999, Integer.MAX_VALUE};

    private final static byte[] DigitTens = {'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '2', '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4',
            '4', '4', '4', '5', '5', '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6',
            '6', '6', '6', '6', '6', '7', '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8',
            '8', '8', '8', '8', '8', '8', '8', '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',};

    private final static byte[] DigitOnes = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
            '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4',
            '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',};

    private final static byte[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
            'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
            't', 'u', 'v', 'w', 'x', 'y', 'z'};

    public RedisOutputStream(final OutputStream out) {
        // 创建默认数据块大小的流
        this(out, OUTPUT_BUFFER_SIZE);
    }


    public RedisOutputStream(final OutputStream out, final int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        buf = new byte[size];
    }

    public void writeIntCrLf(int value) throws IOException {
        // 如果值是负数，先写入 '-' 号，再将值转为正数写入
        if (value < 0) {
            write((byte) '-');
            value = -value;
        }

        // 确实写入这个整数值所需要的字符长度
        int size = 0;
        while (value > sizeTable[size]) {
            size++;
        }
        size++;
        // 如果剩余的值不够，刷新缓冲区
        if (size >= buf.length - count) {
            flushBuffer();
        }

        int q, r;
        int charPos = count + size;

        // 对大于 65536 的值进行处理
        while (value >= 65536) {
            q = value / 100;
            r = value - ((q << 6) + (q << 5) + (q << 2));
            value = q;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // 对小于 65536 的值进行处理
        for (; ; ) {
            q = (value * 52429) >>> (16 + 3);
            r = value - ((q << 3) + (q << 1));
            buf[--charPos] = digits[r];
            value = q;
            if (value == 0) break;
        }
        // 更新缓冲区的位置
        count += size;

        // 写入一个换行符
        writeCrLf();
    }

    public void writeCrLf() throws IOException {
        // 空间不够，刷新缓冲区
        if (2 >= buf.length - count) {
            flushBuffer();
        }

        // 写入 '\r\n' 缓冲符
        buf[count++] = '\r';
        buf[count++] = '\n';
    }

    private void flushBuffer() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }

    public void write(final byte b) throws IOException {
        if (count == buf.length) {
            flushBuffer();
        }
        buf[count++] = b;
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        out.flush();
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (len >= buf.length) {
            flushBuffer();
            out.write(b, off, len);
        } else {
            if (len >= buf.length - count) {
                flushBuffer();
            }

            System.arraycopy(b, off, buf, count, len);
            count += len;
        }
    }
}
