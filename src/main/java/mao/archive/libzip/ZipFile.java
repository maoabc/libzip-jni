package mao.archive.libzip;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipException;

/**
 * Created by mao on 16-10-30.
 */
public class ZipFile implements Closeable {

    public static final int ZIP_CREATE = 1;
    public static final int ZIP_EXCL = 2;
    public static final int ZIP_CHECKCONS = 4;
    public static final int ZIP_TRUNCATE = 8;
    public static final int ZIP_RDONLY = 16;


    static {
        System.loadLibrary("zip-jni");
        initIDs();
    }

    private volatile boolean closeRequested;
    private long jzip;

    private Map<String, ZipEntry> entries;

    private final ZipCoder zc;

    private ProgressListener listener;

    public ZipFile(String name) throws IOException {
        this(name, "UTF-8", ZIP_CREATE);
    }

    public ZipFile(File archive, String charset) throws IOException {
        this(archive.getAbsolutePath(), charset, ZIP_CREATE);
    }

    public ZipFile(String name, String charset, int mode) throws IOException {
        zc = ZipCoder.get(Charset.forName(charset));
        jzip = open(name, mode);
        initEntries();
    }

    private void initEntries() throws IOException {
        ensureOpen();
        int count = (int) getEntriesCount(jzip);
        long jzip = this.jzip;
        Map<String, ZipEntry> entries = new HashMap<>();
        ZipCoder zc = this.zc;
        for (int i = 0; i < count; i++) {
            ZipEntry zipEntry = getEntry0(jzip, i);
            String name = zc.toString(zipEntry.rawName);
            zipEntry.name = name;
            entries.put(name, zipEntry);
        }
        this.entries = entries;
    }


    public Iterable<ZipEntry> entries() {
        if (entries == null) {
            return Collections.emptyList();
        }
        return entries.values();
    }

    public Set<Map.Entry<String, ZipEntry>> entrySet() {
        if (entries == null) {
            return Collections.emptySet();
        }
        return entries.entrySet();
    }

    public boolean hasEntry(String name) {
        return entries != null && entries.containsKey(name);
    }


    public ZipEntry getEntry(String name) {
        if (name == null) {
            throw new NullPointerException("name==null");
        }
        if (entries == null) {
            return null;
        }
        return entries.get(name);
    }


    public synchronized boolean renameZipEntry(ZipEntry entry, String newName) {
        ensureOpen();
        return renameEntry(jzip, entry.index, newName);
    }

    private boolean removeEntry(long index) {
        ensureOpen();
        return removeEntry(jzip, index);
    }

    public synchronized boolean removeEntry(ZipEntry entry) {
        return removeEntry(entry.index);
    }

    public synchronized boolean removeEntry(String name) {
        ZipEntry entry = entries.get(name);
        return entry != null && removeEntry(entry.index);
    }

    public synchronized void addFileEntry(String name, File file) throws IOException {
        ensureOpen();
        addFileEntry(jzip, name, file.getAbsolutePath(), 0, (int) file.length());
    }

    public synchronized void addFileEntry(String entryName, String fileName, int off, int len) throws IOException {
        ensureOpen();
        addFileEntry(jzip, entryName, fileName, off, len);
    }

    public synchronized void addBufferEntry(String name, byte[] buf) throws IOException {
        ensureOpen();

        addBufferEntry(jzip, name, buf);
    }


    public synchronized void addDirectoryEntry(String name) throws IOException {
        ensureOpen();
        addDirectoryEntry(jzip, name);
    }

    public void setPassword(String password) {
        ensureOpen();
        setPassword(jzip, password);
    }


    public InputStream getInputStream(ZipEntry entry) throws IOException {
        if (entry == null) {
            throw new NullPointerException("entry");
        }
        ensureOpen();
        long jzf ;
        synchronized (this) {
            jzf = openEntry(jzip, entry.index);
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
        entries = null;
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

    //打开一个zip文件,返回一个zip_t结构体指针
    private static native long open(String name, int mode) throws IOException;

    //设置zip密码
    private static native void setPassword(long jzip, String password);

    //得到zip内文件的数量
    private static native long getEntriesCount(long jzip);

    //根据索引得到一个zipEntry对象
    private static native ZipEntry getEntry0(long jzip, long index) throws IOException;

    //根据name得到一个zipEntry对象
    private static native ZipEntry getEntry1(long jzip, byte[] rawName) throws IOException;


    //删除zip中一个文件
    private static native boolean removeEntry(long jzip, long index);


    //重命名zip中文件
    private static native boolean renameEntry(long jzip, long index, String newName);

    //添加文件到zip
    private static native void addFileEntry(long jzip, String name, String fileName, int start, int len) throws IOException;


    //添加一个字节数组到zip
    private static native long addBufferEntry(long jzip, String name, byte[] buffer) throws IOException;

    //添加目录到zip
    private static native void addDirectoryEntry(long jzip, String name) throws IOException;


    //打开一个zip内部文件
    //jzip  zip_t结构体指针
    private static native long openEntry(long jzip, long index) throws IOException;

    //从zip内部文件中读取数据
    //jzf    zip_file_t结构体指针
    private static native long readEntryBytes(long jzf, byte[] buf, int off, int len) throws IOException;

    // 从zip内部文件中读取数据,大数据使用
    private static native int readEntryBuffer(long jzf, ByteBuffer buffer, int len) throws IOException;


    //关闭zipEntry

    private static native void closeEntry(long jzf) throws IOException;


    //关闭zip文件
    private static native void close(long jzip, ProgressListener progressListener) throws IOException;


}
