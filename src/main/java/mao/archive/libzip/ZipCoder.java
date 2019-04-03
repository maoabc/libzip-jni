
package mao.archive.libzip;

import java.nio.charset.Charset;

import androidx.annotation.Keep;


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

    byte[] getBytes(String s) {
        return s.getBytes(cs);
    }

}
