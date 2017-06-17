

package mao.archive.libzip;


/**
 * 从java.util.zip.ZipEntry修改来的
 * This class is used to represent a ZIP file entry.
 *
 * @author David Connelly
 */
public class ZipEntry {

    /* encryption methods */
    public static final int ZIP_EM_NONE = 0;  /* not encrypted */
    public static final int ZIP_EM_TRAD_PKWARE = 1; /* traditional PKWARE encryption */
    public static final int ZIP_EM_AES_128 = 0x0101;  /* Winzip AES encryption */
    public static final int ZIP_EM_AES_192 = 0x0102;
    public static final int ZIP_EM_AES_256 = 0x0103;
    public static final int ZIP_EM_UNKNOWN = 0xffff; /* unknown algorithm */


    long index = -1;                 /* index within archive */

    String name;
    byte[] rawName;        // raw entry name
    long mtime = -1;     // last modification time
    long crc = -1;      // crc-32 of entry data
    long size = -1;     // uncompressed size of entry data
    long csize = -1;    // compressed size of entry data
    int method = -1;    // compression method
    int emethod;
    int flag;       // general purpose flag
    byte[] extra;       // optional extra field data for entry
    String comment;     // optional comment string for entry


    /**
     * Compression method for uncompressed entries.
     */
    public static final int STORED = 0;

    /**
     * Compression method for compressed (deflated) entries.
     */
    public static final int DEFLATED = 8;


    /**
     * Creates a new un-initialized zip entry
     */
    public ZipEntry() {

    }

    /**
     * Creates a new zip entry with the specified name.
     *
     * @param name The entry name
     * @throws NullPointerException     if the entry name is null
     * @throws IllegalArgumentException if the entry name is longer than
     *                                  0xFFFF bytes
     */
    public ZipEntry(String name) {
        this.name = name;
    }


    /**
     * Creates a new zip entry with fields taken from the specified
     * zip entry.
     *
     * @param e A zip Entry object
     * @throws NullPointerException if the entry object is null
     */
    public ZipEntry(ZipEntry e) {
        index = e.index;
        rawName = e.rawName;
        name = e.name;
        mtime = e.mtime;
        crc = e.crc;
        size = e.size;
        csize = e.csize;
        method = e.method;
        emethod = e.emethod;
        flag = e.flag;
        extra = e.extra;
        comment = e.comment;
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
     * Sets the last modification time of the entry.
     * <p>
     * <p> If the entry is output to a ZIP file or ZIP file formatted
     * output stream the last modification time set by this method will
     * be stored into the {@code date and time fields} of the zip file
     * entry and encoded in standard {@code MS-DOS date and time format}.
     * The {@link java.util.TimeZone#getDefault() default TimeZone} is
     * used to convert the epoch time to the MS-DOS data and time.
     *
     * @param time The last modification time of the entry in milliseconds
     *             since the epoch
     * @see #getTime()
     */
    public void setTime(long time) {
        this.mtime = time / 1000;
    }

    /**
     * Returns the last modification time of the entry.
     * <p>
     * <p> If the entry is read from a ZIP file or ZIP file formatted
     * input stream, this is the last modification time from the {@code
     * date and time fields} of the zip file entry. The
     * {@link java.util.TimeZone#getDefault() default TimeZone} is used
     * to convert the standard MS-DOS formatted date and time to the
     * epoch time.
     *
     * @return The last modification time of the entry in milliseconds
     * since the epoch, or -1 if not specified
     * @see #setTime(long)
     */
    public long getTime() {
        return mtime != -1 ? mtime * 1000 : -1;
    }


    /**
     * Sets the uncompressed size of the entry data.
     *
     * @param size the uncompressed size in bytes
     * @throws IllegalArgumentException if the specified size is less
     *                                  than 0, is greater than 0xFFFFFFFF when
     *                                  <a href="package-summary.html#zip64">ZIP64 format</a> is not supported,
     *                                  or is less than 0 when ZIP64 is supported
     * @see #getSize()
     */
    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;
    }

    /**
     * Returns the uncompressed size of the entry data.
     *
     * @return the uncompressed size of the entry data, or -1 if not known
     * @see #setSize(long)
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
     * @see #setCompressedSize(long)
     */
    public long getCompressedSize() {
        return csize;
    }

    /**
     * Sets the size of the compressed entry data.
     *
     * @param csize the compressed size to set to
     * @see #getCompressedSize()
     */
    public void setCompressedSize(long csize) {
        this.csize = csize;
    }

    /**
     * Sets the CRC-32 checksum of the uncompressed entry data.
     *
     * @param crc the CRC-32 value
     * @throws IllegalArgumentException if the specified CRC-32 value is
     *                                  less than 0 or greater than 0xFFFFFFFF
     * @see #getCrc()
     */
    public void setCrc(long crc) {
        if (crc < 0 || crc > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid entry crc-32");
        }
        this.crc = crc;
    }

    /**
     * Returns the CRC-32 checksum of the uncompressed entry data.
     *
     * @return the CRC-32 checksum of the uncompressed entry data, or -1 if
     * not known
     * @see #setCrc(long)
     */
    public long getCrc() {
        return crc;
    }

    /**
     * Sets the compression method for the entry.
     *
     * @param method the compression method, either STORED or DEFLATED
     * @throws IllegalArgumentException if the specified compression
     *                                  method is invalid
     * @see #getMethod()
     */
    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = method;
    }

    /**
     * Returns the compression method of the entry.
     *
     * @return the compression method of the entry, or -1 if not specified
     * @see #setMethod(int)
     */
    public int getMethod() {
        return method;
    }

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
     * Sets the optional comment string for the entry.
     * <p>
     * <p>ZIP entry comments have maximum length of 0xffff. If the length of the
     * specified comment string is greater than 0xFFFF bytes after encoding, only
     * the first 0xFFFF bytes are output to the ZIP file entry.
     *
     * @param comment the comment string
     * @see #getComment()
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the comment string for the entry.
     *
     * @return the comment string for the entry, or null if none
     * @see #setComment(String)
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

}
