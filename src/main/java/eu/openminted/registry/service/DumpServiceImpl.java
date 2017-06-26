package eu.openminted.registry.service;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service("dumpService")
public class DumpServiceImpl implements DumpService {


    private static FileAttribute PERMISSIONS = PosixFilePermissions.asFileAttribute(EnumSet.of
            (PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission
                            .OWNER_EXECUTE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission
                            .OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE));

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceTypeService resourceTypeService;

    static void writeZipFile(File directoryToZip, List<File> fileList) {

        try {

            Path filePath = Files.createFile(directoryToZip.toPath(), PERMISSIONS);
            FileOutputStream fos = new FileOutputStream(filePath.toFile());
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addToZip(directoryToZip, file, zos);
                }
            }

            zos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void getAllFiles(File dir, List<File> fileList) {
        try {
            File[] files = dir.listFiles();
            for (File file : files) {
                fileList.add(file);
                if (file.isDirectory()) {
                    System.out.println("directory:" + file.getCanonicalPath());
                    getAllFiles(file, fileList);
                } else {
                    System.out.println("     file:" + file.getCanonicalPath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException,
            IOException {

        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                file.getCanonicalPath().length());
        System.out.println("Writing '" + zipFilePath + "' to zip file");
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

    public File bringAll() {

        Path masterDirectory = null;

        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd") ;
            masterDirectory = Files.createTempDirectory(dateFormat.format(today.getTime()),PERMISSIONS);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        List<ResourceType> resourceTypes;
        List<Resource> resources;

        resourceTypes = resourceTypeService.getAllResourceType();
        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < resourceTypes.size(); i++) {
            if (!resourceTypes.get(i).getName().equals("user")) {
                resources = resourceService.getResource(resourceTypes.get(i).getName());
                createDirectory(masterDirectory.toAbsolutePath().toString() + "/" + resourceTypes.get(i).getName(), resources);
                try {
                    File tempFile = new File(masterDirectory + "/" + resourceTypes.get(i).getName() + ".json");
                    Path filePath = Files.createFile(tempFile.toPath(), PERMISSIONS);
                    FileWriter file = new FileWriter(filePath.toFile());
                    file.write(Utils.objToJson(resourceTypes.get(i)));
                    file.flush();
                    file.close();
                } catch (IOException e) {
                    new ServiceException("Failed to create schema-file for " + resourceTypes.get(i).getName());
                }
            }
        }
        File tempDir = masterDirectory.toFile();
        getAllFiles(tempDir, fileList);
        File masterZip = new File(masterDirectory + "/final.zip");
        writeZipFile(masterZip, fileList);
        try {
            File tempFile = new File(masterDirectory + "/dump-" + getCurrentDate() + ".zip");
            Files.createFile(tempFile.toPath(), PERMISSIONS);
            masterZip.renameTo(tempFile);
			return tempFile;
        } catch (IOException e1) {

        }
        return masterZip;
    }

    public File bringResourceType(String resourceType) {


        String parentName = "/home/user/tmp/dump-testCase";
        File masterDirectory = new File(parentName);

        try {
            Files.createDirectory(masterDirectory.toPath(), PERMISSIONS);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        ResourceType resourceTypes = new ResourceType();
        List<Resource> resources;

        resourceTypes = resourceTypeService.getResourceType(resourceType);
        List<File> fileList = new ArrayList<>();
        if (resourceTypes != null) {
            resources = resourceService.getResource(resourceTypes.getName());
            createDirectory(parentName + "/" + resourceTypes.getName(), resources);
            try {
                File tempFile = new File(parentName + "/" + resourceTypes.getName() + ".json");
                Path filePath = Files.createFile(tempFile.toPath(), PERMISSIONS);
                FileWriter file = new FileWriter(filePath.toFile());
                file.write(Utils.objToJson(resourceTypes));
                file.flush();
                file.close();
            } catch (IOException e) {
                new ServiceException("Failed to create schema-file for " + resourceTypes.getName());
            }
        }
        File tempDir = new File(parentName);
        getAllFiles(tempDir, fileList);
        File masterZip = new File(parentName + "/final.zip");
        writeZipFile(masterZip, fileList);
        try {
            File tempFile = new File(parentName + "/dump-" + getCurrentDate() + ".zip");
            Files.createFile(tempFile.toPath(), PERMISSIONS);
            masterZip.renameTo(tempFile);
//			return tempFile;
        } catch (IOException e1) {

        }
        return masterZip;
    }

    public void createDirectory(String name, List<Resource> resources) {
        File parentDirectory = new File(name);

        if (!parentDirectory.exists()) {
            try {
                Files.createDirectory(parentDirectory.toPath(), PERMISSIONS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < resources.size(); i++) {
            try {
                File openFile = new File(name + "/" + resources.get(i).getId() + ".json");
                Path filePath = Files.createFile(openFile.toPath(), PERMISSIONS);
                FileWriter file = new FileWriter(filePath.toFile());
                file.write(Utils.objToJson(resources.get(i)));
                file.flush();
                file.close();
            } catch (IOException e) {
//				new ServiceException("Failed to create file(s) for "+ name);
                e.printStackTrace();
            }
        }

    }

    public String getCurrentDate() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("ddMMyyyy");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
}
