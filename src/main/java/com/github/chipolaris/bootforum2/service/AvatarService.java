package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.Avatar;
import com.github.chipolaris.bootforum2.domain.FileInfo;
import com.github.chipolaris.bootforum2.dto.AvatarDTO;
import com.github.chipolaris.bootforum2.dto.FileCreatedDTO;
import com.github.chipolaris.bootforum2.dto.FileResourceDTO;
import com.github.chipolaris.bootforum2.mapper.AvatarMapper;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import com.github.chipolaris.bootforum2.repository.AvatarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AvatarService {

    private static final Logger logger = LoggerFactory.getLogger(AvatarService.class);

    private final AvatarRepository avatarRepository;
    private final FileService fileService;
    private final FileInfoMapper fileInfoMapper;
    private final AvatarMapper avatarMapper;
    private final AuthenticationFacade authenticationFacade;

    public AvatarService(AvatarRepository avatarRepository, FileService fileService,
                         FileInfoMapper fileInfoMapper, AvatarMapper avatarMapper,
                         AuthenticationFacade authenticationFacade) {
        this.avatarRepository = avatarRepository;
        this.fileService = fileService;
        this.fileInfoMapper = fileInfoMapper;
        this.avatarMapper = avatarMapper;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Handles uploading and persisting a user's avatar.
     * If an avatar already exists for the user, it will be replaced.
     *
     * @param avatarFile The avatar image file from the multipart request.
     * @return A ServiceResponse containing the AvatarDTO or an error message.
     */
    @Transactional
    public ServiceResponse<AvatarDTO> uploadAvatar(MultipartFile avatarFile) {

        String username = authenticationFacade.getCurrentUsername()
                .orElse(null);

        if (username == null) {
            return ServiceResponse.failure("User not authenticated. Cannot upload avatar.");
        }

        if (avatarFile == null || avatarFile.isEmpty()) {
            return ServiceResponse.failure("Avatar file is empty or not provided.");
        }

        // 1. Store the new file using the existing FileService
        ServiceResponse<FileCreatedDTO> fileResponse = fileService.storeFile(avatarFile);
        if (fileResponse.isFailure()) {
            logger.warn("Failed to store avatar file for user {}. Reason: {}", username, fileResponse.getMessages());
            return ServiceResponse.failure("Failed to store avatar file: " + String.join(", ", fileResponse.getMessages()));
        }

        FileInfo newFileInfo = fileInfoMapper.toEntity(fileResponse.getDataObject());
        // Note: FileInfo is not persisted yet, it will be persisted via cascade from Avatar

        // 2. Find existing avatar or create a new one
        Optional<Avatar> existingAvatarOpt = avatarRepository.findByUserName(username);

        Avatar avatar;
        if (existingAvatarOpt.isPresent()) {
            avatar = existingAvatarOpt.get();

            fileService.deleteFile(avatar.getFile().getId()); // Delete the old file

            logger.info("Updating existing avatar for user '{}'", username);
            // The old FileInfo entity will be deleted due to orphanRemoval=true on Avatar.file
            avatar.setFile(newFileInfo);
        } else {
            logger.info("Creating new avatar for user '{}'", username);
            avatar = new Avatar();
            avatar.setUserName(username);
            avatar.setFile(newFileInfo);
            avatar.setCreateBy(username);
        }
        avatar.setUpdateBy(username);

        // 3. Persist the Avatar entity (which cascades to FileInfo)
        Avatar savedAvatar = avatarRepository.save(avatar);

        AvatarDTO avatarDTO = avatarMapper.toDTO(savedAvatar);

        return ServiceResponse.success("Avatar uploaded successfully.", avatarDTO);
    }

    @Transactional(readOnly = true)
    public ServiceResponse<AvatarDTO> getAvatar() {
        String username = authenticationFacade.getCurrentUsername()
                .orElse(null);
        if (username == null) {
            return ServiceResponse.failure("User not authenticated. Cannot get avatar.");
        }

        logger.info("Retrieving avatar for user '{}'", username);
        return avatarRepository.findByUserName(username)
                .map(avatar -> ServiceResponse.success("Found avatar for user %s".formatted(username), avatarMapper.toDTO(avatar)))
                .orElse(ServiceResponse.failure("Avatar not found"));
    }

    /**
     * Retrieves the avatar for a specific user as a resource.
     *
     * @param username The username of the user whose avatar is requested.
     * @return the avatar associated with username, or the file default-avatar.png if not found.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<FileResourceDTO> getAvatarResource(String username) {
        return avatarRepository.findByUserName(username).map(avatar -> {
            FileInfo avatarFileInfo = avatar.getFile();
            FileResourceDTO fileResourceDTO = fileService.getFileResourceById(avatarFileInfo.getId()).getDataObject();
            return ServiceResponse.success("Found avatar resource for user %s".formatted(username), fileResourceDTO);
        }).orElseGet(() -> {
            return ServiceResponse.success("Avatar not found for user %s. Return the default avatar".formatted(username),
                new FileResourceDTO(new ClassPathResource("static/images/default-avatar.png"), "default-avatar.png", "image/png"));
        }
        );
    }

    @Transactional(readOnly = true)
    public ServiceResponse<Long> getAvatarFileId(String userName) {
        return avatarRepository.findByUserName(userName).map(avatar -> {
            FileInfo avatarFileInfo = avatar.getFile();
            return ServiceResponse.success("Found avatar file id for user %s".formatted(userName), avatarFileInfo.getId());
        }).orElseGet( () -> {
            /* return success anyway with null payload */
            return ServiceResponse.success("Avatar doesn't exist for user %s".formatted(userName), null);
        });
    }

    @Transactional(readOnly = true)
    public ServiceResponse<Map<String, Long>> getAvatarFileIds(List<String> userNames) {
        return ServiceResponse.success("Retrieve avatar file ids",
                avatarRepository.findAvatarFileIdsByUserNames(userNames));
    }
}