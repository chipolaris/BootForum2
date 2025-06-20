package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.FileResourceDTO;
import com.github.chipolaris.bootforum2.service.FileStorageService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/api/public/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long fileId, HttpServletRequest request) {
        logger.debug("Received request to serve file with ID: {}", fileId);

        ServiceResponse<FileResourceDTO> serviceResponse =
                fileStorageService.getFileResourceById(fileId);

        if (serviceResponse.isFailure() || serviceResponse.getDataObject() == null) {
            logger.warn("Failed to load file for serving with ID {}: {}", fileId, serviceResponse.getMessages());
            // Determine if it's a 404 or 500 based on service messages if needed
            if (serviceResponse.getMessages().stream().anyMatch(m -> m.contains("not found"))) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        FileResourceDTO fileResourceDTO = serviceResponse.getDataObject();
        Resource resource = fileResourceDTO.resource();

        long contentLength = 0;
        try {
            contentLength = resource.contentLength();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        String originalFilename = fileResourceDTO.originalFilename();

        String mimeType = fileResourceDTO.mimeType();

        // Final fallback if content type is not available
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        logger.info("Serving file '{}' with content type '{}'", originalFilename, mimeType);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                .contentLength(contentLength)
                .body(resource);
    }
}
