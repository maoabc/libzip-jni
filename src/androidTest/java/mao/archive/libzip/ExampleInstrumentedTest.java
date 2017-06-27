package mao.archive.libzip;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("mao.archive.libzip.test", appContext.getPackageName());
    }

    @Test
    public void testGetZipEntry() throws IOException {
        ZipFile zipFile = new ZipFile("/sdcard/en_test.zip");
        ZipEntry entry = zipFile.getEntry(0);
        ZipEntry entry1 = zipFile.getEntry("abc.txt");
        System.out.println(entry);
    }

    @Test
    public void testCreateZip() throws IOException {
        ZipFile zipFile = new ZipFile("/sdcard/test_create.zip");
        zipFile.addBufferEntry("abc", "hello world".getBytes());
        zipFile.close();

    }

    @Test
    public void testCreateZipFromFile() throws IOException {
        ZipFile zipFile = new ZipFile("/sdcard/test_create.zip", Charset.forName("UTF-8"), ZipFile.ZIP_CREATE);
        zipFile.addFileEntry("rarlng.rar", new File("/sdcard/rarlng_android.rar"));
        zipFile.close();
    }

    @Test
    public void testCreateEncryptZip() throws IOException {
        ZipFile zipFile = new ZipFile("/sdcard/test_create_encrypt.zip");
        zipFile.setPassword("190512");
        long idx = zipFile.addBufferEntry("abc", "hello world".getBytes());
        zipFile.setEntryEncryptionMethod(idx, ZipEntry.ZIP_EM_AES_128);
        zipFile.close();

    }

    @Test
    public void testExtractEncryptedFile() throws IOException {
        ZipFile zipFile = new ZipFile(new File("/sdcard/en23_test.zip"), Charset.forName("UTF-8"), ZipFile.ZIP_RDONLY);
        zipFile.setPassword("19051203");
        for (ZipEntry zipEntry : zipFile.entries()) {
            System.out.println(zipEntry.getName());
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
            String s;
            while ((s = reader.readLine()) != null) {
                System.out.println(s);
            }
        }
        zipFile.close();
    }

    @Test
    public void testListEntryCharset() throws IOException {
        ZipFile zipFile = new ZipFile(new File("/sdcard/Baidu-PCS-SD-SDK-Android-L2-1.0.0.zip"), Charset.forName("UTF-8"), ZipFile.ZIP_RDONLY);
        String comment = zipFile.getComment();
        for (ZipEntry zipEntry : zipFile.entries()) {
            String name = zipEntry.getName();
            System.out.println(name);
        }
        zipFile.close();
    }
}
