package com.truethic.soninx.SoniNxAPI.fileConfig;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;

@Service
public class FileStorageService {
    private Path fileStorageLocation;

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else return "";
    }

    public String storeFile(MultipartFile file, FileStorageProperties fileStorageProperties) {

        File newfile = convertMultiPartFileToFile(file);
        if (newfile != null) {
            this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                    .toAbsolutePath().normalize();
            try {
                Files.createDirectories(this.fileStorageLocation);
            } catch (Exception ex) {
                throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
            }
            // Normalize file name
//            String fileName = StringUtils.cleanPath(file.getOriginalFilename());

            Calendar date = Calendar.getInstance();
            long millisecondsDate = date.getTimeInMillis();

            String uniqueFileName =
                    millisecondsDate + "." + getFileExtension(newfile);
            System.out.println("uniqueFileName " + uniqueFileName);

           /* else
            uniqueFileName = uniqueFileName.substring(0, uniqueFileName.lastIndexOf("."))+fileRename;
           */
            //String uniqueFileName1 = StringUtils.cleanPath(file.getName());

            try {
                // Check if the file's name contains invalid characters
                if (uniqueFileName.contains("..")) {
                    throw new FileStorageException("Sorry! Filename contains invalid path sequence " + uniqueFileName);
                }
                // Copy file to the target location (Replacing existing file with the same name)
                Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
                Resource resource = new UrlResource(targetLocation.toUri());
         /*   System.out.println("\nParent Path:"+targetLocation.getParent());
            System.out.println("\nFile System Path:"+targetLocation.getFileSystem());
            System.out.println("\nFile Name:"+targetLocation.getFileName());
            System.out.println("\nFile URI :"+targetLocation.toUri());
            System.out.println("\nURL Path:"+resource.getURL());
            System.out.println("\nFile Name:"+resource.getFilename());
            System.out.println("\nFile URI :"+resource.getURI());
            System.out.println("\nAbsolute Path:"+targetLocation.toAbsolutePath());*/
                Long fileResult = Files.copy(file.getInputStream(), targetLocation,
                        StandardCopyOption.REPLACE_EXISTING);
                if (fileResult != null) {
                    if (newfile.exists()) {
                        System.out.println("deleted file");
                        newfile.delete(); //remove file from local directory
                    }
                }
                return uniqueFileName;
            } catch (IOException ex) {
                throw new FileStorageException("Could not store file " + uniqueFileName + ". Please try again!", ex);
            }
        } else
            return "file not found";
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found " + fileName, ex);
        }
    }

    private File convertMultiPartFileToFile(final MultipartFile multipartFile) {
        final File file = new File(multipartFile.getOriginalFilename());
        try (final FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(multipartFile.getBytes());
        } catch (final IOException ex) {
            //  LOGGER.error("Error converting the multi-part file to file= ", ex.getMessage());
        }
        return file;
    }
}
