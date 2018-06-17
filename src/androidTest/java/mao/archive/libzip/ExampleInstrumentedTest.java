package mao.archive.libzip;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
