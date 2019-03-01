package mao.archive.libzip;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import static mao.archive.libzip.ZipFile.ZIP_CM_DEFLATE;
import static mao.archive.libzip.ZipFile.ZIP_EM_AES_128;
import static mao.archive.libzip.ZipFile.ZIP_RDONLY;
import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private Context appContext;

    @Before
    public void useAppContext() throws Exception {
        // Context of the app under test.
        appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("mao.archive.libzip.test", appContext.getPackageName());
    }

    /**
     * Copy input stream to output stream without closing streams. Flushes output stream when done.
     *
     * @param is input stream
     * @param os output stream
     * @throws IOException for any error
     */
    public static void copyStream(InputStream is, OutputStream os)
            throws IOException {
        try {
            if (is == null) {
                return;
            }
            if (os == null) {
                return;
            }
            byte[] buff = new byte[8 * 1024];
            int rc;
            while ((rc = is.read(buff)) != -1) {
                os.write(buff, 0, rc);
            }
            os.flush();
        } finally {
            if (is != null) is.close();
            if (os != null) os.close();
        }
    }

    @Test
    public void testCreateZip() throws IOException {
        File file = new File(appContext.getCacheDir(), "test_create.zip");
        ZipFile zipFile = new ZipFile(file, "UTF-8");
        String name = "test.txt";
        zipFile.addBytes(name, "hello world".getBytes());
        zipFile.close();

        zipFile = new ZipFile(file, "UTF-8", ZipFile.ZIP_RDONLY);
        ZipEntry entry = zipFile.getEntry(0);
        ZipEntry entry1 = zipFile.getEntry(name);
        System.out.println(entry + "  " + entry1);

    }


    @Test
    public void testCompression() throws IOException {
        String password = "123abc";

        File file = new File(appContext.getCacheDir(), "test_create.zip");
        file.delete();

        ZipFile zipFile = new ZipFile(file, "UTF-8");
        long i = zipFile.addFile("magisk.zip", new File("/sdcard/Magisk.zip"));
        zipFile.setDefaultPassword(password);

        zipFile.setCompressionMethod(i, ZIP_CM_DEFLATE, 0);

        zipFile.setEncryptionMethod(i, ZipFile.ZIP_EM_AES_256);

        zipFile.close(new ProgressListener() {
            @Override
            public void onProgressing(int percent) {
                System.out.println("progress " + percent);
            }
        });

        zipFile = new ZipFile(file, "UTF-8", ZipFile.ZIP_RDONLY);
        ZipEntry entry = zipFile.getEntry(0);
        long compressedSize = entry.getCompressedSize();
        long size = entry.getSize();
        double d = ((double) compressedSize) / size;
        System.out.println(size + "  " + compressedSize + "   " + d);

    }

    @Test
    public void testPassword() throws IOException {
        File file = new File(appContext.getCacheDir(), "/sdcard/test_create.zip");

        ZipFile zipFile = new ZipFile(file, "UTF-8");

        String password = zipFile.getDefaultPassword();

        assert password == null;

        String p = "123abc";
        zipFile.setDefaultPassword(p);
        password = zipFile.getDefaultPassword();

        zipFile.close();

        assertEquals(password, p);

        file.delete();

    }

    @Test
    public void testCreateZipFromFile() throws IOException {
        ZipFile zipFile = new ZipFile("/sdcard/test_create.zip", "UTF-8", ZipFile.ZIP_CREATE);
        zipFile.addFile("rarlng.rar", new File("/sdcard/abc.doc"));
        zipFile.close();
    }

    @Test
    public void testCreateEncryptZip() throws IOException {
        File file = new File(appContext.getCacheDir(), "test_encrypt.zip");

        ZipFile zipFile = new ZipFile(file);
        zipFile.setDefaultPassword("123abc");

        long idx = zipFile.addBytes("abc", "hello world".getBytes());
        zipFile.setEncryptionMethod(idx, ZIP_EM_AES_128);
        zipFile.close();

        zipFile = new ZipFile(file, "UTF-8", ZIP_RDONLY);
        try {
            InputStream inputStream = zipFile.getInputStream(0, "aaaaa");

        } catch (IOException e) {
            assert e instanceof WrongPasswordException;
        }

    }

    @Test
    public void multiThreadRead() throws IOException, NoSuchAlgorithmException, InterruptedException {
        File file = new File(appContext.getCacheDir(), "multi.zip");
        ZipFile zf = new ZipFile(file);
        String[] ns = {"a", "b", "c", "d", "e"};
        for (String n : ns) {
            InputStream is = getClass().getResourceAsStream("/" + n);
            File f = new File(appContext.getCacheDir(), n);
            copyStream(is, new FileOutputStream(f));
            zf.addFile(n, f);
        }
        zf.close();

        List<Thread> threads=new ArrayList<>();
        final ZipFile zipFile = new ZipFile(file);
        for (final ZipEntry entry : zipFile.entries()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream is1 = zipFile.getInputStream(entry);
                        System.out.println("open "+entry.getName());
                        InputStream is2 = getClass().getResourceAsStream("/" + entry.getName());
                        byte[] d1 = digest(is1);
                        byte[] d2 = digest(is2);
                        System.out.println(Arrays.toString(d1)+"   "+Arrays.toString(d2));
                        assertEquals(Arrays.toString(d1),Arrays.toString(d2));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            thread.join();
        }

    }

    private static boolean compStream(InputStream is1, InputStream is2) throws NoSuchAlgorithmException, IOException {
        byte[] d1 = digest(is1);
        byte[] d2 = digest(is2);
        System.out.println(Arrays.toString(d1)+"   "+Arrays.toString(d2));
        return Arrays.equals(d1, d2);
    }

    private static byte[] digest(InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        byte[] bytes = new byte[8 * 1024];
        int size;
        while ((size = is.read(bytes)) != -1) {
            sha1.update(bytes, 0, size);
        }
        is.close();
        return sha1.digest();
    }

    @Test
    public void testExtractEncryptedFile() throws IOException {
        ZipFile zipFile = new ZipFile(new File("/sdcard/abc.zip"), "UTF-8", ZipFile.ZIP_RDONLY);
        zipFile.setDefaultPassword("123abc");
        for (ZipEntry zipEntry : zipFile.entries()) {
            System.out.println(zipEntry.getName());
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String s;
            while ((s = reader.readLine()) != null) {
                System.out.println(s);
            }
        }
        zipFile.close();
    }

    @Test
    public void testListEntryCharset() throws IOException {
//        ZipFile zipFile = new ZipFile(new File("/sdcard/Baidu-PCS-SD-SDK-Android-L2-1.0.0.zip"), Charset.forName("UTF-8"), ZipFile.ZIP_RDONLY);
//        String comment = zipFile.getZipComment();
//        for (ZipEntry zipEntry : zipFile.entries()) {
//            String name = zipEntry.getPath();
//            System.out.println(name);
//        }
//        zipFile.close();
    }
}
