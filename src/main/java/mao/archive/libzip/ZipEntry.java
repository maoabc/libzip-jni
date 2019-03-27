

package mao.archive.libzip;


import androidx.annotation.Keep;

import static mao.archive.libzip.ZipFile.ZIP_EM_NONE;

/**
 * 从java.util.zip.ZipEntry修改来的
 * This class is used to represent a ZIP file entry.
 */
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
     * @return
     */
    public boolean isValid() {
        return index != -1;
    }


    /**
     * Returns the name of the entry.
     *
     * @return the name of the entry
     */
    public String getName() {
        return name;
    }


    /**
     * Returns the last modification time of the entry.
     *
     * @return The last modification time of the entry in milliseconds
     * since the epoch, or -1 if not specified
     */
    public long getTime() {
        return mtime;
    }


    /**
     * Returns the uncompressed size of the entry data.
     *
     * @return the uncompressed size of the entry data, or -1 if not known
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the size of the compressed entry data.
     * <p>
     * <p> In the case of a stored entry, the compressed size will be the same
     * as the uncompressed size of the entry.
     *
     * @return the size of the compressed entry data, or -1 if not known
     */
    public long getCompressedSize() {
        return csize;
    }


    /**
     * Returns the CRC-32 checksum of the uncompressed entry data.
     *
     * @return the CRC-32 checksum of the uncompressed entry data, or -1 if
     * not known
     */
    public long getCrc() {
        return crc;
    }


    /**
     * Returns the compression method of the entry.
     */
    public int getMethod() {
        return method;
    }

    /**
     * Returns the encryption method of the entry.
     */
    public int getEmethod() {
        return emethod;
    }


    /**
     * Returns the extra field data for the entry.
     *
     * @return the extra field data for the entry, or null if none
     */
    public byte[] getExtra() {
        return extra;
    }


    /**
     * Returns the comment string for the entry.
     *
     * @return the comment string for the entry, or null if none
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns true if this is a directory entry. A directory entry is
     * defined to be one whose name ends with a '/'.
     *
     * @return true if this is a directory entry
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

    /**
     * Returns a string representation of the ZIP entry.
     */
    public String toString() {
        return getName();
    }


    /**
     * Returns the hash code value for this entry.
     */
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
