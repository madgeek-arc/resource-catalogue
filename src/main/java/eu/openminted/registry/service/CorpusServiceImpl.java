package eu.openminted.registry.service;

import eu.openminted.registry.domain.Corpus;
import eu.openminted.store.restclient.StoreRESTClient;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by stefanos on 15-Nov-16.
 */
@Service("corpusService")
@Primary
public class CorpusServiceImpl extends AbstractGenericService<Corpus> implements CorpusService {

    private Logger logger = Logger.getLogger(CorpusServiceImpl.class);

    private final int BUFFER_SIZE = 4096;

    @Autowired
    Environment environment;

    public CorpusServiceImpl() {
        super(Corpus.class);
    }

    @Override
    public String getResourceType() {
        return "corpus";
    }


    @Override
    public String uploadCorpus(String filename, InputStream inputStream) {
        String archiveId = null;

        try {
            StoreRESTClient storeClient = new StoreRESTClient(environment.getProperty("services.store.ip", "http://83.212.101.85:8090"));
            File temp = File.createTempFile("copr", "tmp");
            OutputStream fos = new BufferedOutputStream(new FileOutputStream(temp));

            IOUtils.copyLarge(inputStream, fos);
            fos.flush();
            fos.close();

            archiveId = storeClient.createArchive().getResponse();

            logger.info("Creating archiveId " + archiveId);
            logger.info("Creating subarchive " + storeClient.createSubArchive(archiveId, "metadata").getResponse());
            logger.info("Creating subarchive " + storeClient.createSubArchive(archiveId, "fulltext").getResponse());
            logger.info("Creating subarchive " + storeClient.createSubArchive(archiveId, "abstract").getResponse());

            /*
               * unzip file
               * iterate through its directories
               * upload each file according to the corresponding directory
             */

            String destDirectory = archiveId;
            File destDir = new File(destDirectory);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(temp));
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                File unzippedFile = new File(filePath);

                if (!entry.isDirectory()) {
                    new File(unzippedFile.getParent()).mkdirs();
                    unzippedFile.createNewFile();
                    // if the entry is a file, extracts it
                    extractFile(zipIn, unzippedFile);
                } else {
                    // if the entry is a directory, make the directory
                    unzippedFile.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();

            if (destDir.listFiles() != null)
                for (File file : destDir.listFiles()) {
                    iterateThroughDirectories(storeClient, archiveId, file, file.getParent());
                }

            logger.info("Done uploading files");

            storeClient.finalizeArchive(archiveId);

            FileDeleteStrategy.FORCE.delete(temp);
            FileDeleteStrategy.FORCE.delete(destDir);

        } catch (IOException e) {
            logger.error("Error uploading corpus", e);
        }

        return archiveId;
    }

    @Override
    public InputStream downloadCorpus(String archiveId) {
        try {
            StoreRESTClient storeClient = new StoreRESTClient(environment.getProperty("services.store.ip", "http://83.212.101.85:8090"));
            File temp = File.createTempFile("cor", "tmp");

            temp.deleteOnExit();
            storeClient.downloadArchive(archiveId, temp.getAbsolutePath());
            return new FileInputStream(temp);
        } catch (Exception e) {
            logger.error("error downloading file", e);
        }

        return null;
    }

    private void extractFile(ZipInputStream zipIn, File file) throws IOException {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
            bos.close();
        } catch (FileNotFoundException e) {
            logger.error("Error reading file while unzipping", e);
        }
    }

    private void iterateThroughDirectories(StoreRESTClient storeClient, String archiveId, File file, String parent) throws IOException {
        if (file.getName().matches("^\\.[_|A-Z|a-z|0-9].*")
                || file.getName().matches(".*[M|m][A|a][C|c][O|o][S|s][X|x].*")) {

            FileDeleteStrategy.FORCE.delete(file);

            return;
        }

        if (file.isDirectory()) {
            if (file.listFiles() == null) return;
            for (File child : file.listFiles()) {
                if (child == null) continue;
                iterateThroughDirectories(storeClient, archiveId, child, file.getName());
            }
        } else {
            if (parent.equalsIgnoreCase("metadata")
                    || parent.equalsIgnoreCase("fulltext")
                    || parent.equalsIgnoreCase("abstract")) {
                storeClient.storeFile(file, archiveId + File.separator + parent, file.getName());
                logger.info("Uploading " + archiveId + File.separator + parent + File.separator + file.getName());
            }
        }
    }

}
