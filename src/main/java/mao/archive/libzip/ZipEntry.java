

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

    public static final int ZIP_CM_DEFAULT = -1; /* better of deflate or store */
    public static final int ZIP_CM_STORE = 0; /* stored (uncompressed) */
    public static final int ZIP_CM_DEFLATE = 8; /* deflated */


    long index = -1;                 /* index within archive */

    String name;
    long mtime = -1;     // last modification time
    long crc = -1;      // crc-32 of entry data
    long size;     // uncompressed size of entry data
    long csize;    // compressed size of entry data
    int method = ZIP_CM_DEFAULT;    // compression method
    int emethod = ZIP_EM_NONE;
    int flags;       // general purpose flags
    byte[] extra;       // optional extra field data for entry
    String comment;     // optional comment string for entry


    /**
     * Creates a new un-initialized zip entry
     */
    ZipEntry() {

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
     * entry是否被层初始化
     *
     * @return
     */
    public boolean isValid() {
        return index != -1;
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
        name = e.name;
        mtime = e.mtime;
        crc = e.crc;
        size = e.size;
        csize = e.csize;
        method = e.method;
        emethod = e.emethod;
        flags = e.flags;
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
     *
     */
    public int getMethod() {
        return method;
    }

    /**
     * Returns the encryption method of the entry.
     *
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
