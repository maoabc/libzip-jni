
package mao.archive.libzip;

import androidx.annotation.Keep;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;


@Keep
final class ZipCoder {


    private Charset cs;

    private ZipCoder(Charset cs) {
        this.cs = cs;
    }

    public static ZipCoder get(Charset charset) {
        return new ZipCoder(charset);
    }

    public String toString(byte[] ba) {
        if (ba == null) {
            return null;
        }
        return new String(ba, 0, ba.length, cs);
    }

    // \0 end
    byte[] getBytes(String s) {
        ByteBuffer buffer = cs.encode(s);
        int limit = buffer.limit();
        byte[] bytes = new byte[limit + 1];
        buffer.get(bytes, 0, limit);
        bytes[limit] = 0;
        return bytes;
    }

}
