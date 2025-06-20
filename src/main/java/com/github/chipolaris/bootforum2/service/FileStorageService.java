package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.FileInfo;
import com.github.chipolaris.bootforum2.dto.FileCreatedDTO;
import com.github.chipolaris.bootforum2.dto.FileInfoDTO;
import com.github.chipolaris.bootforum2.dto.FileResourceDTO;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
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
    private final FileInfoMapper fileInfoMapper;
    private final GenericDAO genericDAO;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public FileStorageService(@Value("${file.storage.base-path}") String storagePath,
                              FileInfoMapper fileInfoMapper, GenericDAO genericDAO) {
        this.fileStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        this.fileInfoMapper = fileInfoMapper;
        this.genericDAO = genericDAO;
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

    public ServiceResponse<FileCreatedDTO> storeFile(MultipartFile multipartFile) {

        if (multipartFile == null || multipartFile.isEmpty()) {
            return ServiceResponse.error("File is empty or not provided.");
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

            // Create FileCreatedDTO
            FileCreatedDTO fileInfo = new FileCreatedDTO(originalFilename, multipartFile.getContentType(),
                    multipartFile.getSize(), datePartitionPath.toString() + "/" + uniqueFilename);

            logger.info("Stored file '{}' as '{}'", originalFilename, uniqueFilename);

            return ServiceResponse.success("File stored successfully: " + originalFilename, fileInfo);

        } catch (IOException ex) {
            logger.error("Could not store file {}. Please try again!", originalFilename, ex);
            return ServiceResponse.error("Could not store file " + originalFilename + ". Error: " + ex.getMessage());
        }
    }

    /**
     * Loads a file as a resource along with its metadata, ready for serving.
     *
     * @param fileId The ID of the FileInfo entity.
     * @return ServiceResponse containing FileServingResourceDTO or an error.
     */
    public ServiceResponse<FileResourceDTO> getFileResourceById(Long fileId) {
        // 1. Get FileInfo to retrieve metadata
        FileInfo fileInfo = genericDAO.find(FileInfo.class, fileId);

        // 2. Resolve path and create resource (similar to existing loadFileAsResource)
        try {
            Path filePath = this.fileStorageLocation.resolve(fileInfo.getPath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                long contentLength = resource.contentLength(); // Get content length
                FileResourceDTO fileResourceDTO = new FileResourceDTO(resource,
                        fileInfo.getOriginalFilename(), fileInfo.getMimeType());

                logger.debug("Prepared file for serving (original name {}): {}", fileInfo.getOriginalFilename(), filePath);
                return ServiceResponse.success("File resource created successfully.", fileResourceDTO);
            } else {
                logger.warn("File resource not found or not readable at path: {} for file ID: {}", filePath, fileId);
                return ServiceResponse.error("File not found or is not readable: " + fileInfo.getOriginalFilename());
            }
        } catch (MalformedURLException ex) {
            logger.error("Error creating URL resource for file (original name {}): {} for file ID: {}",
                    fileInfo.getOriginalFilename(), fileInfo.getPath(), fileId, ex);
            return ServiceResponse.error("Error reading file: " + fileInfo.getOriginalFilename() + ". Invalid path.");
        } catch (IOException ex) { // For resource.fileSize()
            logger.error("Error getting content length for file (original name {}): {} for file ID: {}",
                    fileInfo.getOriginalFilename(), fileInfo.getPath(), fileId, ex);
            return ServiceResponse.error("Error accessing file details: " + fileInfo.getOriginalFilename());
        }
    }

    /**
     * Delete a file
     *
     * @param fileId id of the file to delete
     * @return ServiceResponse indicating the outcome.
     */

    public ServiceResponse<Void> deleteFile(Long fileId) {

        FileInfo fileInfo = genericDAO.find(FileInfo.class, fileId);

        if(fileInfo == null) {
            return ServiceResponse.error("File not found for deletion");
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

            return ServiceResponse.success("File and its metadata deleted successfully: " + fileInfo.getOriginalFilename());

        } catch (IOException ex) {
            logger.error("Could not delete file with ID {}", fileInfo.getId(), ex);
            return ServiceResponse.error("Could not delete file " + fileInfo.getOriginalFilename() + ". Error: " + ex.getMessage());
        }
    }
}