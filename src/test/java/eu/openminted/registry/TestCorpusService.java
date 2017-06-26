package eu.openminted.registry;

import eu.openminted.store.restclient.StoreRESTClient;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TestCorpusService {

    private static final int BUFFER_SIZE = 4096;

    @Test
    @Ignore
    public void unzipFolder() throws Exception {

        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("corpus.zip");

        if (fileUrl == null) return;

        File inputFile = new File(fileUrl.getFile());

        String destDirectory = "tmpDirectory";

        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(inputFile));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }

        zipIn.close();

        if (destDir.listFiles() != null) {
            for (File file : destDir.listFiles()) {
                iterateThroughDirectories(file, file.getParent());
            }
        }
        System.out.println("Done");
    }

    @Test
    @Ignore
    public void testStoreClient() throws Exception {
        InputStream inputStream = null;
        String archiveId = null;
        StoreRESTClient storeClient = new StoreRESTClient("http://83.212.101.85:8090");
        File temp = File.createTempFile("copr", "tmp");
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(temp));
        archiveId = storeClient.createArchive().getReport();
        storeClient.createSubArchive(archiveId, "metadata");
        storeClient.createSubArchive(archiveId, "fullText");
        storeClient.createSubArchive(archiveId, "abstract");
        IOUtils.copyLarge(inputStream, fos);
        fos.flush();
        fos.close();
    }


    private void iterateThroughDirectories(File file, String parent) throws IOException {
        if (file.getName().contains(".DS_Store")
                || file.getName().contains("__MACOSX")) {
            FileDeleteStrategy.FORCE.delete(file);
            return;
        }

        System.out.println(parent + File.separator + file.getName());

        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                iterateThroughDirectories(child, file.getName());
            }
        }
    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
