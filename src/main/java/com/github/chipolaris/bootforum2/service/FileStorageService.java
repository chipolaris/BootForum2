package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.FileInfo;
import com.github.chipolaris.bootforum2.dto.FileInfoDTO;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path fileStorageLocation;
    private final GenericDAO genericDAO;
    private final FileInfoMapper fileInfoMapper;

    @Autowired
    public FileStorageService(@Value("${file.storage.base-path}") String storagePath,
                              GenericDAO genericDAO, FileInfoMapper fileInfoMapper) {
        this.fileStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        this.genericDAO = genericDAO;
        this.fileInfoMapper = fileInfoMapper;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("Created file storage directory at: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            logger.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Transactional
    public ServiceResponse<FileInfoDTO> storeFile(MultipartFile multipartFile) {
        ServiceResponse<FileInfoDTO> response = new ServiceResponse<>();

        if (multipartFile == null || multipartFile.isEmpty()) {
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("File is empty or not provided.");
        }

        String originalFilename = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFilename);
        // Keep UUID for unique filename, but partitioning will be date-based
        String uniqueFilenameBase = UUID.randomUUID().toString();
        String uniqueFilename = uniqueFilenameBase + (extension != null ? "." + extension : "");

        try {
            // --- Date-based Partitioning Strategy ---
            LocalDate today = LocalDate.now();
            String year = String.valueOf(today.getYear());
            // Format month and day with leading zeros (e.g., 01, 02, ..., 12)
            String month = today.format(DateTimeFormatter.ofPattern("MM"));
            String day = today.format(DateTimeFormatter.ofPattern("dd"));

            Path datePartitionPath = Paths.get(year, month, day);
            Path targetDirectory = this.fileStorageLocation.resolve(datePartitionPath);

            Files.createDirectories(targetDirectory); // Create date-based partition directories if they don't exist

            Path targetLocation = targetDirectory.resolve(uniqueFilename);

            try (InputStream inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Save FileInfo entity
            FileInfo fileInfo = new FileInfo();
            fileInfo.setOriginalFilename(originalFilename);
            fileInfo.setMimeType(multipartFile.getContentType());
            // Store the relative path from the base storage location, including date partitions
            fileInfo.setPath(datePartitionPath.resolve(uniqueFilename).toString().replace("\\", "/")); // Ensure consistent path separators

            genericDAO.persist(fileInfo);

            logger.info("Stored file '{}' as '{}' with relative path '{}'", originalFilename, uniqueFilename, fileInfo.getPath());

            return response.setDataObject(fileInfoMapper.toDTO(fileInfo))
                    .addMessage("File stored successfully: " + originalFilename);

        } catch (IOException ex) {
            logger.error("Could not store file {}. Please try again!", originalFilename, ex);
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Could not store file " + originalFilename + ". Error: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<Resource> loadFileAsResource(Long fileId) {
        ServiceResponse<Resource> response = new ServiceResponse<>();

        FileInfo fileInfo = genericDAO.find(FileInfo.class, fileId);

        if (fileInfo == null) {
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("File not found with ID: " + fileId);
        }

        try {
            Path filePath = this.fileStorageLocation.resolve(fileInfo.getPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                logger.debug("Loading resource for file ID {}: {}", fileId, filePath);
                return response.setDataObject(resource)
                        .addMessage("File resource loaded successfully.");
            } else {
                logger.warn("File resource not found or not readable at path: {}", filePath);
                return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                        .addMessage("File not found or is not readable: " + fileInfo.getOriginalFilename());
            }
        } catch (MalformedURLException ex) {
            logger.error("Error creating URL resource for file ID {}: {}", fileId, fileInfo.getPath(), ex);
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Error reading file: " + fileInfo.getOriginalFilename() + ". Invalid path.");
        }
    }

    /**
     * Optional: Method to delete a file and its metadata.
     *
     * @param fileId The ID of the file to delete.
     * @return ServiceResponse indicating the outcome.
     */
    @Transactional
    public ServiceResponse<Void> deleteFile(Long fileId) {
        ServiceResponse<Void> response = new ServiceResponse<>();

        FileInfo fileInfo = genericDAO.find(FileInfo.class, fileId);

        if (fileInfo == null) {
            return response.setAckCode(ServiceResponse.AckCodeType.WARNING) // Warning as file metadata doesn't exist
                    .addMessage("File metadata not found with ID: " + fileId + ". No action taken.");
        }

        try {
            Path filePath = this.fileStorageLocation.resolve(fileInfo.getPath()).normalize();
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                logger.info("Successfully deleted file from filesystem: {}", filePath);
            } else {
                logger.warn("File not found on filesystem for deletion, but metadata existed: {}", filePath);
                // Proceed to delete metadata anyway or handle as an inconsistency
            }

            genericDAO.remove(fileInfo);
            logger.info("Successfully deleted file metadata for ID: {}", fileId);

            return response.addMessage("File and its metadata deleted successfully: " + fileInfo.getOriginalFilename());

        } catch (IOException ex) {
            logger.error("Could not delete file with ID {}: {}", fileId, fileInfo.getPath(), ex);
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Could not delete file " + fileInfo.getOriginalFilename() + ". Error: " + ex.getMessage());
        }
    }
}