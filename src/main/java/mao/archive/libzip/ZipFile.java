package mao.archive.libzip;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.zip.ZipException;

/**
 * Created by mao on 16-10-30.
 */
public class ZipFile implements Closeable {

    /*file open mode*/
    public static final int ZIP_CREATE = 1;       //Create the archive if it does not exist.
    public static final int ZIP_EXCL = 2;         //Error if archive already exists.
    public static final int ZIP_CHECKCONS = 4;    //Perform additional stricter consistency checks on the archive, and error if they fail.
    public static final int ZIP_TRUNCATE = 8;     //If archive exists, ignore its current contents.  In other words, handle it the same way as an empty archive.
    public static final int ZIP_RDONLY = 16;      //Open archive in read-only mode.


    /* encryption methods */
    public static final int ZIP_EM_NONE = 0;  /* not encrypted */

    //不再支持此加密创建文件，但可以解压
    @Deprecated
    public static final int ZIP_EM_TRAD_PKWARE = 1; /* traditional PKWARE encryption */

    public static final int ZIP_EM_AES_128 = 0x0101;  /* Winzip AES encryption */
    public static final int ZIP_EM_AES_192 = 0x0102;
    public static final int ZIP_EM_AES_256 = 0x0103;
    public static final int ZIP_EM_UNKNOWN = 0xffff; /* unknown algorithm */

    public static final int ZIP_CM_DEFAULT = -1; /* better of deflate or store */
    public static final int ZIP_CM_STORE = 0; /* stored (uncompressed) */
    public static final int ZIP_CM_DEFLATE = 8; /* deflated */
    //暂时不用
    public static final int ZIP_CM_BZIP2 = 12; /* compressed using BZIP2 algorithm */


    static {
        System.loadLibrary("zip-jni");
    }

    private volatile boolean closeRequested;
    private long jzip = 0L;
    private final String path;
    private final String charset;


    private final ZipCoder zc;

    private ProgressListener listener;


    public ZipFile(File archive) throws IOException {
        this(archive.getCanonicalPath(), "UTF-8", ZIP_CREATE);
    }

    public ZipFile(File archive, String charset) throws IOException {
        this(archive.getCanonicalPath(), charset, ZIP_CREATE);
    }

    public ZipFile(File archive, String charset, int mode) throws IOException {
        this(archive.getCanonicalPath(), charset, mode);
    }

    public ZipFile(String path, String charset, int mode) throws IOException {

        if (charset == null)
            throw new NullPointerException("charset is null");
        zc = ZipCoder.get(Charset.forName(charset));
        this.charset = charset;
        jzip = open(path, mode);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getCharset() {
        return charset;
    }

    public ZipEntry getEntry(String name) {
        if (name == null) {
            throw new NullPointerException("path==null");
        }
        synchronized (this) {
            ensureOpen();
            long index = nameLocate0(jzip, zc.getBytes(name));
            if (index != -1) {
                return getEntry0(jzip, zc, index);
            }
        }
        return null;
    }

    public ZipEntry getEntry(long index) {
        if (index == -1) {
            throw new NullPointerException("index == -1");
        }
        synchronized (this) {
            ensureOpen();
            return getEntry0(jzip, zc, index);
        }
    }


    public Iterable<ZipEntry> entries() {
        synchronized (this) {
            ensureOpen();
            long num = getEntriesCount(jzip);
            return new IterEntry((int) num);
        }
    }

    private class IterEntry implements Iterable<ZipEntry> {
        private final int nums;

        IterEntry(int nums) {
            this.nums = nums;
        }

        @Override
        public Iterator<ZipEntry> iterator() {
            return new Iterator<ZipEntry>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < nums;
                }

                @Override
                public ZipEntry next() {
                    return getEntry0(jzip, zc, index++);
                }
            };
        }
    }

    public boolean hasEntry(String name) {
        if (name == null) {
            throw new NullPointerException("path==null");
        }
        synchronized (this) {
            ensureOpen();
            long index = nameLocate0(jzip, zc.getBytes(name));
            return index != -1;
        }
    }


    public boolean rename(ZipEntry entry, String newName) {
        if (rename(entry.index, newName)) {
            return true;
        }
        return false;
    }

    public boolean rename(long index, String newName) {
        synchronized (this) {
            ensureOpen();
            return renameEntry(jzip, index, zc.getBytes(newName));
        }
    }

    private boolean remove(long index) {
        if (index == -1) {
            throw new IllegalArgumentException("index invalid ");
        }
        synchronized (this) {
            ensureOpen();
            return removeEntry0(jzip, index);
        }
    }

    public boolean remove(ZipEntry entry) {
        return remove(entry.index);
    }

    public long nameLocate(String name) {
        synchronized (this) {
            ensureOpen();
            return nameLocate0(jzip, zc.getBytes(name));
        }
    }

    public boolean remove(String name) {
        long index = nameLocate(name);
        return index != -1 && remove(index);
    }


    public long addFile(String name, File file) throws IOException {
        return addFile(name, file.getAbsolutePath(), 0, file.length(), ZIP_EM_NONE, ZIP_CM_DEFAULT, 0);
    }

    public long addFile(String name, File file, int em, int cm, int level) throws IOException {
        return addFile(name, file.getAbsolutePath(), 0, file.length(), em, cm, level);
    }

    public long addFile(String name, String fileName, long off, long len, int em, int cm, int level) throws IOException {
        synchronized (this) {
            ensureOpen();
            long index = addFileEntry0(jzip, zc.getBytes(name), fileName, off, len);
            if (index != -1) {
                setEncryptionMethod(index, em);
                setCompressionMethod(index, cm, level);
            }
            return index;
        }
    }

    public long addBytes(String name, byte[] buf) throws IOException {
        return addBytes(name, buf, ZIP_EM_NONE, ZIP_CM_DEFAULT, 0);
    }

    public long addBytes(String name, byte[] buf, int em, int cm, int level) throws IOException {
        synchronized (this) {
            ensureOpen();
            long index = addBufferEntry0(jzip, zc.getBytes(name), buf);
            if (index != -1) {
                setEncryptionMethod(index, em);
                setCompressionMethod(index, cm, level);
            }
            return index;
        }
    }


    public void addDirectory(String name) throws IOException {
        synchronized (this) {
            ensureOpen();
            addDirectoryEntry0(jzip, zc.getBytes(name));
        }
    }

    public boolean setEncryptionMethod(long index, int em) {
        return setEncryptionMethod(index, em, null);

    }

    public boolean setEncryptionMethod(long index, int em, String password) {
        checkSupportedEncryptionMethod(em);
        if (index == -1) {
            throw new IllegalArgumentException("index invalid ");
        }
        synchronized (this) {
            ensureOpen();
            return setEncryptionMethod0(jzip, index, em, password);
        }
    }

    public boolean setModifyTime(long index, long time) {
        if (index == -1) {
            throw new IllegalArgumentException("index invalid");
        }
        synchronized (this) {
            ensureOpen();
            return setModifyTime0(jzip, index, time / 1000);
        }
    }

    public boolean setCompressionMethod(long index, int cm) {
        return setCompressionMethod(index, cm, 0);
    }

    public boolean setCompressionMethod(long index, int cm, int level) {
        checkSupportedCompressionMethod(cm, level);
        synchronized (this) {
            ensureOpen();
            return setCompressionMethod0(jzip, index, cm, level);
        }
    }

    private static void checkSupportedCompressionMethod(int cm, int level) {
        if (level < 0 || level > 9) {
            throw new IllegalArgumentException("compression method level [0-9], current is " + level);
        }
        switch (cm) {
            case ZIP_CM_DEFAULT:
            case ZIP_CM_DEFLATE:
            case ZIP_CM_STORE:
            case ZIP_CM_BZIP2:
                return;
        }
        throw new IllegalArgumentException("invalid compression method");
    }

    private static void checkSupportedEncryptionMethod(int em) {
        switch (em) {
            case ZIP_EM_NONE:
            case ZIP_EM_AES_128:
            case ZIP_EM_AES_192:
            case ZIP_EM_AES_256:
                return;
        }
        throw new IllegalArgumentException("invalid encryption method");
    }


    public boolean setComment(long index, String comment) {
        if (comment == null) {
            throw new NullPointerException("comment");
        }
        synchronized (this) {
            ensureOpen();
            return setComment0(jzip, index, zc.getBytes(comment));
        }

    }


    public String getDefaultPassword() {
        synchronized (this) {
            ensureOpen();
            return getDefaultPassword0(jzip);
        }
    }

    /**
     * Sets the default password used when accessing encrypted files
     *
     * @param password
     */
    public void setDefaultPassword(String password) {
        synchronized (this) {
            ensureOpen();
            setDefaultPassword0(jzip, password);
        }
    }


    public String getZipComment() {
        synchronized (this) {
            ensureOpen();
            byte[] comment = getZipComment0(jzip);
            if (comment != null) {
                return zc.toString(comment);
            }
        }
        return null;
    }

    /**
     * Sets the comment for the entire zip archive
     *
     * @param comment
     * @return
     */
    public boolean setZipComment(String comment) {
        if (comment == null || "".equals(comment)) {
            return false;
        }
        synchronized (this) {
            ensureOpen();
            return setZipComment0(jzip, zc.getBytes(comment));
        }
    }


    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return getInputStream(entry, null);
    }


    public InputStream getInputStream(ZipEntry entry, String password) throws IOException {
        if (entry == null) {
            throw new NullPointerException("entry");
        }
        return getInputStream(entry.index, password);
    }

    public InputStream getInputStream(long index, String password) throws IOException {
        synchronized (this) {
            ensureOpen();
            long jzf = openEntry(jzip, index, password);
            ZipEntry entry = getEntry0(jzip, zc, index);
            return new ZipFileInputStream(jzf, entry.getSize());
        }
    }


    private void ensureOpen() {
        if (closeRequested) {
            throw new IllegalStateException("Zip file closed");
        }

        if (jzip == 0) {
            throw new IllegalStateException("The object is not initialized.");
        }
    }

    private void ensureOpenOrZipException() throws IOException {
        if (closeRequested) {
            throw new ZipException("ZipFile closed");
        }
    }

    @Override
    public void close() throws IOException {
        close(listener);
        listener = null;
    }

    public void close(ProgressListener listener) throws IOException {
        if (closeRequested)
            return;
        closeRequested = true;
        close0(jzip, listener);
        jzip = 0;
    }

    public void discard() {
        if (closeRequested)
            return;
        closeRequested = true;
        discard0(jzip);
        jzip = 0;
    }

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    private class ZipFileInputStream extends InputStream {
        private volatile boolean closeRequested = false;
        private long jzf; // address of jzf data
        private long rem;     // number of remaining bytes within entry
        private long size;    // uncompressed size of this entry

        ZipFileInputStream(long jzf, long size) {
            this.jzf = jzf;
            this.size = rem = size;
        }

        public int read(byte b[], int off, int len) throws IOException {
            synchronized (ZipFile.this) {
                if (rem == 0) {
                    return -1;
                }
                if (len <= 0) {
                    return 0;
                }
                if (len > rem) {
                    len = (int) rem;
                }
                ensureOpenOrZipException();
                len = (int) ZipFile.readEntryBytes(jzf, b, off, len);
                if (len > 0) {
                    rem -= len;
                }
            }
            if (rem == 0) {
                close();
            }
            return len;
        }

        public int read() throws IOException {
            byte[] b = new byte[1];
            if (read(b, 0, 1) == 1) {
                return b[0] & 0xff;
            } else {
                return -1;
            }
        }

        public long skip(long n) throws IOException {
            if (n > rem)
                n = rem;
            rem -= n;
            if (rem == 0) {
                close();
            }
            return n;
        }

        public int available() {
            return rem > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rem;
        }

        public long size() {
            return size;
        }

        public void close() throws IOException {
            if (closeRequested)
                return;
            closeRequested = true;

            rem = 0;
            synchronized (ZipFile.this) {
                if (jzf != 0 && ZipFile.this.jzip != 0) {
                    closeEntry(jzf);
                    jzf = 0;
                }
            }
        }

        protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close(null);
        } finally {
            super.finalize();
        }
    }

    /**
     * Opens the zip archive specified by path and returns a pointer to a struct zip
     *
     * @param path zip archive path
     * @param mode open mode
     * @return a struct zip pointer
     * @throws IOException
     */
    private static native long open(String path, int mode) throws IOException;

    //得到zip内文件的数量
    private static native long getEntriesCount(long jzip);


    private static native void setDefaultPassword0(long jzip, String password);

    private static native String getDefaultPassword0(long jzip);

    /**
     * The file at position index in the zip archive archive is marked as deleted
     *
     * @param jzip  specifies the zip archive
     * @param index index in the zip archive
     * @return
     */
    private static native boolean removeEntry0(long jzip, long index);


    /**
     * The file at position index in the zip archive archive is renamed to path
     *
     * @param jzip  specifies the zip archive
     * @param index index in the zip archive
     * @param name  new path
     * @return
     */
    private static native boolean renameEntry(long jzip, long index, byte[] name);


    /**
     * Adds a file to a zip archive
     *
     * @param jzip     specifies the zip archive
     * @param name     the file's path in the zip archive
     * @param fileName file path
     * @param start    offset start
     * @param len      read length
     * @return the index of the new file in the archive
     * @throws IOException
     */
    private static native long addFileEntry0(long jzip, byte[] name, String fileName, long start, long len) throws IOException;


    //添加一个字节数组到zip

    /**
     * Adds a file to a zip archive
     *
     * @param jzip   specifies the zip archive
     * @param name   the file's path in the zip archive
     * @param buffer a zip source from the buffer data
     * @return the index of the new file in the archive
     * @throws IOException
     */
    private static native long addBufferEntry0(long jzip, byte[] name, byte[] buffer) throws IOException;


    /**
     * Adds a directory to a zip archive.
     *
     * @param jzip specifies the zip archive
     * @param name the directory's path in the zip archive
     * @return the index of the new entry in the archive
     * @throws IOException
     */
    private static native long addDirectoryEntry0(long jzip, byte[] name) throws IOException;

    /*returns the index of the file named rawName in archive*/
    private static native long nameLocate0(long jzip, byte[] rawName);


    private static native ZipEntry getEntry0(long jzip, ZipCoder zc, long index);


    /*Sets zip entry */
    private static native boolean setModifyTime0(long jzip, long index, long time);

    private static native boolean setEncryptionMethod0(long jzip, long index, int emethod, String password);

    private static native boolean setCompressionMethod0(long jzip, long index, int cm, int level);

    private static native boolean setComment0(long jzip, long index, byte[] comment);


    private static native byte[] getZipComment0(long jzip);

    private static native boolean setZipComment0(long jzip, byte[] comment);

    /**
     * Opens the file at position index
     *
     * @param jzip  specifies the zip archive
     * @param index index in the zip archive
     * @return a struct zip_file pointer
     * @throws IOException
     */
    private static native long openEntry(long jzip, long index, String password) throws IOException;


    /**
     * Reads at most len bytes from file into buf,start offset off
     *
     * @param jzf specifies the zip archive
     * @param buf buffer data
     * @param off offset in buf
     * @param len bytes len
     * @return the number of bytes actually read is returned.  Otherwise, -1 is returned
     * @throws IOException
     */
    private static native long readEntryBytes(long jzf, byte[] buf, int off, int len) throws IOException;


    //Closes file in archive and frees the memory allocated for it
    private static native void closeEntry(long jzf) throws IOException;

    private static native void discard0(long jzip);

    //Closes archive and frees the memory allocated for it
    private static native void close0(long jzip, ProgressListener progressListener) throws IOException;


}
