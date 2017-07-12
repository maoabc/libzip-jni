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


    static {
        System.loadLibrary("zip-jni");
        initIDs();
    }

    private volatile boolean closeRequested;
    private long jzip;
    private final String name;
    private final String charsetName;


    private final ZipCoder zc;

    private ProgressListener listener;
    private String password;
    /*the compression method*/
    private int compressionMethod = ZipEntry.ZIP_CM_DEFAULT;

    /*the encryption method*/
    private int encryptionMethod = ZipEntry.ZIP_EM_NONE;

    public ZipFile(String name) throws IOException {
        this(name, Charset.forName("UTF-8"), ZIP_CREATE);
    }

    public ZipFile(File archive, String charset) throws IOException {
        this(archive.getAbsolutePath(), Charset.forName(charset), ZIP_CREATE);
    }

    public ZipFile(File archive, Charset charset, int mode) throws IOException {
        this(archive.getPath(), charset, mode);
    }

    public ZipFile(String name, Charset charset, int mode) throws IOException {

        if (charset == null)
            throw new NullPointerException("charset is null");
        zc = ZipCoder.get(charset);
        charsetName = charset.name();
        jzip = open(name, mode);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCharsetName() {
        return charsetName;
    }

    public ZipEntry getEntry(long index) {
        if (index == -1) {
            throw new IllegalArgumentException("index == -1");
        }
        synchronized (this) {
            ensureOpen();
            long jstat = openEntryStat(jzip, index);
            if (jstat != 0) {
                ZipEntry entry = getZipEntry(null, jstat);
                entry.index = index;
                freeEntryStat(jstat);
                byte[] comment = getEntryComment(jzip, index);
                if (comment == null) {
                    entry.comment = null;
                } else {
                    entry.comment = zc.toString(comment);
                }
                return entry;
            }
        }
        return null;
    }

    private ZipEntry getZipEntry(String name, long jstat) {
        ZipEntry e = new ZipEntry();
        if (name != null) {
            e.name = name;
        } else {
            byte[] rawName = getEntryName(jstat);
            e.name = zc.toString(rawName);
        }
        long time = getEntryMTime(jstat);
        e.mtime = time == -1 ? time : time * 1000;
        e.csize = getEntryCSize(jstat);
        e.size = getEntrySize(jstat);
        e.crc = getEntryCrc(jstat);
        e.method = getEntryMethod(jstat);
        e.emethod = getEntryEncryptionMethod(jstat);
        e.flags = getEntryFlags(jstat);
        return e;
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
                    return getEntry(index++);
                }
            };
        }
    }

    public boolean hasEntry(String name) {
        synchronized (this) {
            ensureOpen();
            long index = nameLocate(jzip, zc.getBytes(name));
            return index != -1;
        }
    }


    public ZipEntry getEntry(String name) {
        if (name == null) {
            throw new NullPointerException("name==null");
        }
        synchronized (this) {
            ensureOpen();
            long index = nameLocate(jzip, zc.getBytes(name));
            if (index == -1) {
                return null;
            }
            long jstat = openEntryStat(jzip, index);
            if (jstat != 0) {
                ZipEntry entry = getZipEntry(name, jstat);
                entry.index = index;
                freeEntryStat(jstat);
                byte[] comment = getEntryComment(jzip, index);
                if (comment == null) {
                    entry.comment = null;
                } else {
                    entry.comment = zc.toString(comment);
                }
                return entry;
            }
        }
        return null;
    }


    public boolean renameZipEntry(ZipEntry entry, String newName) {
        if (renameZipEntry(entry.index, newName)) {
            entry.name = newName;
            return true;
        }
        return false;
    }

    public boolean renameZipEntry(long index, String newName) {
        synchronized (this) {
            ensureOpen();
            return renameEntry(jzip, index, zc.getBytes(newName));
        }
    }

    private boolean removeEntry(long index) {
        synchronized (this) {
            ensureOpen();
            return removeEntry(jzip, index);
        }
    }

    public boolean removeEntry(ZipEntry entry) {
        return removeEntry(entry.index);
    }

    public long nameLocate(String name) {
        synchronized (this) {
            ensureOpen();
            return nameLocate(jzip, zc.getBytes(name));
        }
    }

    public boolean removeEntry(String name) {
        long index = nameLocate(name);
        return index != -1 && removeEntry(index);
    }

    public long addFileEntry(String name, File file) throws IOException {
        synchronized (this) {
            ensureOpen();
            return addFileEntry(jzip, zc.getBytes(name), file.getAbsolutePath(), 0, file.length());
        }
    }

    public long addFileEntry(String entryName, String fileName, int off, int len) throws IOException {
        synchronized (this) {
            ensureOpen();
            return addFileEntry(jzip, zc.getBytes(entryName), fileName, off, len);
        }
    }

    public long addBufferEntry(String name, byte[] buf) throws IOException {
        synchronized (this) {
            ensureOpen();
            return addBufferEntry(jzip, zc.getBytes(name), buf);
        }
    }


    public void addDirectoryEntry(String name) throws IOException {
        synchronized (this) {
            ensureOpen();
            addDirectoryEntry(jzip, zc.getBytes(name));
        }
    }

    public boolean setEntryEncryptionMethod(long index, int emethod) {
        if (index == -1 || (password == null && emethod != ZipEntry.ZIP_EM_NONE)) {
            throw new IllegalArgumentException("index invalid or do not set password");
        }
        synchronized (this) {
            ensureOpen();
            return setEntryEncryptionMethod(jzip, index, emethod, password);
        }
    }

    public boolean setEntryModifyTime(long index, long time) {
        if (index == -1) {
            throw new IllegalArgumentException("index invalid");
        }
        synchronized (this) {
            ensureOpen();
            return setEntryMTime(jzip, index, time / 1000);
        }
    }

    public boolean setEntryMethod(long index, int method) {
        if (method != ZipEntry.ZIP_CM_DEFAULT && method != ZipEntry.ZIP_CM_DEFLATE && method != ZipEntry.ZIP_CM_STORE) {
            throw new IllegalArgumentException("invalid compression method");
        }
        synchronized (this) {
            ensureOpen();
            return setEntryMethod(jzip, index, method);
        }
    }

    public boolean setEntryComment(long index, String comment) {
        if (comment == null) {
            throw new NullPointerException("comment");
        }
        synchronized (this) {
            ensureOpen();
            return setEntryComment(jzip, index, zc.getBytes(comment));
        }

    }


    /**
     * Sets the default password used when accessing encrypted files
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getComment() {
        synchronized (this) {
            ensureOpen();
            byte[] comment = getComment(jzip);
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
    public boolean setComment(String comment) {
        if (comment == null || "".equals(comment)) {
            return false;
        }
        synchronized (this) {
            ensureOpen();
            return setComment(jzip, zc.getBytes(comment));
        }
    }


    public InputStream getInputStream(ZipEntry entry) throws IOException {
        if (entry == null) {
            throw new NullPointerException("entry");
        }
        long jzf;
        synchronized (this) {
            ensureOpen();
            jzf = openEntry(jzip, entry.index, password);
        }
        return new ZipFileInputStream(jzf, entry.getSize());
    }

    public InputStream getInputStream(ZipEntry entry, String password) throws IOException {
        if (entry == null) {
            throw new NullPointerException("entry");
        }
        long jzf;
        synchronized (this) {
            ensureOpen();
            jzf = openEntry(jzip, entry.index, password);
        }
        return new ZipFileInputStream(jzf, entry.getSize());
    }


    private void ensureOpen() {
        if (closeRequested) {
            throw new IllegalStateException("zip file closed");
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

    private void close(ProgressListener listener) throws IOException {
        if (closeRequested)
            return;
        closeRequested = true;
        close(jzip, listener);
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
            close();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close(null);
    }

    //初始化一些数据
    private static native void initIDs();

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

    /**
     * The file at position index in the zip archive archive is marked as deleted
     *
     * @param jzip  specifies the zip archive
     * @param index index in the zip archive
     * @return
     */
    private static native boolean removeEntry(long jzip, long index);


    /**
     * The file at position index in the zip archive archive is renamed to name
     *
     * @param jzip  specifies the zip archive
     * @param index index in the zip archive
     * @param name  new name
     * @return
     */
    private static native boolean renameEntry(long jzip, long index, byte[] name);


    /**
     * Adds a file to a zip archive
     *
     * @param jzip     specifies the zip archive
     * @param name     the file's name in the zip archive
     * @param fileName file name
     * @param start    offset start
     * @param len      read length
     * @return the index of the new file in the archive
     * @throws IOException
     */
    private static native long addFileEntry(long jzip, byte[] name, String fileName, long start, long len) throws IOException;


    //添加一个字节数组到zip

    /**
     * Adds a file to a zip archive
     *
     * @param jzip   specifies the zip archive
     * @param name   the file's name in the zip archive
     * @param buffer a zip source from the buffer data
     * @return the index of the new file in the archive
     * @throws IOException
     */
    private static native long addBufferEntry(long jzip, byte[] name, byte[] buffer) throws IOException;


    /**
     * Adds a directory to a zip archive.
     *
     * @param jzip specifies the zip archive
     * @param name the directory's name in the zip archive
     * @return the index of the new entry in the archive
     * @throws IOException
     */
    private static native long addDirectoryEntry(long jzip, byte[] name) throws IOException;

    /*returns the index of the file named rawName in archive*/
    private static native long nameLocate(long jzip, byte[] rawName);

    //native struct zip_stat
    private static native long openEntryStat(long jzip, long index);

    private static native byte[] getEntryName(long jstat);

    private static native long getEntrySize(long jstat);

    private static native long getEntryCSize(long jstat);

    private static native long getEntryMTime(long jstat);

    private static native long getEntryCrc(long jstat);

    private static native int getEntryMethod(long jstat);

    private static native int getEntryEncryptionMethod(long jstat);

    private static native int getEntryFlags(long jstat);

    private static native void freeEntryStat(long jstat);


    private static native byte[] getEntryComment(long jzip, long index);

    /*Sets zip entry */
    private static native boolean setEntryMTime(long jzip, long index, long time);

    private static native boolean setEntryEncryptionMethod(long jzip, long index, int emethod, String password);

    private static native boolean setEntryMethod(long jzip, long index, int method);

    private static native boolean setEntryComment(long jzip, long index, byte[] comment);


    private static native byte[] getComment(long jzip);

    private static native boolean setComment(long jzip, byte[] comment);

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


    //Closes archive and frees the memory allocated for it
    private static native void close(long jzip, ProgressListener progressListener) throws IOException;


}
