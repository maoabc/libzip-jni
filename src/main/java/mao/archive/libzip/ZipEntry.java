
package mao.archive.libzip;


import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import static mao.archive.libzip.ZipFile.ZIP_EM_NONE;

@Keep
public class ZipEntry {


    final long index;                 /* index within archive */

    private final String name;
    private final long mtime;     // last modification time
    private final long crc;      // crc-32 of entry data
    private final long size;     // uncompressed size of entry data
    private final long csize;    // compressed size of entry data
    private final int method;    // compression method
    private final int emethod;
    private final byte[] extra;       // optional extra field data for entry
    private final String comment;     // optional comment string for entry


    @Keep
    public ZipEntry(long index, ZipCoder zc, byte[] name, long mtime, long crc, long size, long csize,
                    int method, int emethod, byte[] extra, byte[] comment) {
        this.index = index;
        this.name = zc.toString(name);
        this.mtime = mtime;
        this.crc = crc;
        this.size = size;
        this.csize = csize;
        this.method = method;
        this.emethod = emethod;
        this.extra = extra;
        this.comment = zc.toString(comment);
    }

    /**
     * Creates a new zip entry with the specified name.
     *
     * @param name The entry name
     */
    public ZipEntry(String name) {
        this.index = -1;
        this.name = name;
        this.mtime = 0;
        this.crc = 0;
        this.size = 0;
        this.csize = 0;
        this.method = 0;
        this.emethod = 0;
        this.extra = null;
        this.comment = null;

    }

    /**
     * entry是否在原zip里
     *
     * @return 是否从已打开的zip文件中读取的
     */
    public boolean isValid() {
        return index != -1;
    }


    public String getName() {
        return name;
    }


    /**
     * @return last modified time
     */
    public long getTime() {
        return mtime;
    }


    /**
     * @return 未压缩的文件大小
     */
    public long getSize() {
        return size;
    }

    /**
     * @return 压缩后文件大小
     */
    public long getCompressedSize() {
        return csize;
    }


    /**
     * @return crc32校验
     */
    public long getCrc() {
        return crc;
    }


    /**
     * @return 压缩方法
     */
    public int getMethod() {
        return method;
    }

    /**
     * Returns the encryption method of the entry.
     */
    public int getEncryptedMethod() {
        return emethod;
    }


    /**
     * @return 附加数据
     */
    public byte[] getExtra() {
        return extra;
    }


    /**
     * @return 注释
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return 是否为目录，以是否以"/"结尾判断
     */
    public boolean isDirectory() {
        return name.endsWith("/");
    }


    /**
     * 判断是否加密
     *
     * @return
     */
    public boolean isEncrypted() {
        return emethod != ZIP_EM_NONE;
    }

    @NonNull
    public String toString() {
        return getName();
    }


    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZipEntry zipEntry = (ZipEntry) o;

        return name.equals(zipEntry.name);

    }
}
