package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.Tag;
import com.github.chipolaris.bootforum2.dto.TagCreateDTO;
import com.github.chipolaris.bootforum2.dto.TagDTO;
import com.github.chipolaris.bootforum2.dto.TagUpdateDTO;
import com.github.chipolaris.bootforum2.mapper.TagMapper;
import com.github.chipolaris.bootforum2.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagService(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<List<TagDTO>> getAllTags() {
        try {
            List<Tag> tags = tagRepository.findAllByOrderBySortOrderAsc();
            List<TagDTO> tagDTOs = tags.stream().map(tagMapper::toDTO).collect(Collectors.toList());
            return ServiceResponse.success("Successfully fetched all tags", tagDTOs);
        } catch (Exception e) {
            logger.error("Error fetching all tags", e);
            return ServiceResponse.failure("An unexpected error occurred while fetching tags.");
        }
    }

    public ServiceResponse<TagDTO> createTag(TagCreateDTO tagCreateDTO) {
        try {
            Tag newTag = new Tag();
            newTag.setLabel(tagCreateDTO.label());
            newTag.setIcon(tagCreateDTO.icon());
            newTag.setIconColor(tagCreateDTO.iconColor());
            newTag.setDisabled(false); // New tags are active by default

            // Set sort order to be the last
            long count = tagRepository.count();
            newTag.setSortOrder((int) count + 1);

            Tag savedTag = tagRepository.save(newTag);
            return ServiceResponse.success("Successfully created tag", tagMapper.toDTO(savedTag));
        } catch (Exception e) {
            logger.error("Error creating tag", e);
            return ServiceResponse.failure("An unexpected error occurred while creating the tag.");
        }
    }

    public ServiceResponse<TagDTO> updateTag(TagUpdateDTO tagUpdateDTO) {
        try {
            Optional<Tag> optionalTag = tagRepository.findById(tagUpdateDTO.id());
            if (optionalTag.isEmpty()) {
                return ServiceResponse.failure("Tag not found with ID: " + tagUpdateDTO.id());
            }

            Tag tag = optionalTag.get();
            tag.setLabel(tagUpdateDTO.label());
            tag.setIcon(tagUpdateDTO.icon());
            tag.setIconColor(tagUpdateDTO.iconColor());
            tag.setDisabled(tagUpdateDTO.disabled());

            Tag updatedTag = tagRepository.save(tag);
            return ServiceResponse.success("Successfully updated tag", tagMapper.toDTO(updatedTag));
        } catch (Exception e) {
            logger.error(String.format("Error updating tag with ID %d", tagUpdateDTO.id()), e);
            return ServiceResponse.failure("An unexpected error occurred while updating the tag.");
        }
    }

    public ServiceResponse<Void> deleteTag(Long id) {
        try {
            if (!tagRepository.existsById(id)) {
                return ServiceResponse.failure("Tag not found with ID: " + id);
            }
            // Note: This will fail if the tag is associated with any discussions due to foreign key constraints.
            // A more robust implementation would check for associations first.
            tagRepository.deleteById(id);
            return ServiceResponse.success("Successfully deleted tag");
        } catch (Exception e) {
            logger.error(String.format("Error deleting tag with ID %d", id), e);
            return ServiceResponse.failure("An error occurred. The tag might be in use by discussions.");
        }
    }

    public ServiceResponse<Void> updateTagOrder(List<Long> orderedIds) {
        try {
            List<Tag> allTags = tagRepository.findAll();
            Map<Long, Tag> tagMap = allTags.stream().collect(Collectors.toMap(Tag::getId, Function.identity()));

            for (int i = 0; i < orderedIds.size(); i++) {
                Long tagId = orderedIds.get(i);
                Tag tag = tagMap.get(tagId);
                if (tag != null) {
                    tag.setSortOrder(i);
                }
            }
            tagRepository.saveAll(tagMap.values());
            return ServiceResponse.success("Successfully updated tag order");
        } catch (Exception e) {
            logger.error("Error updating tag order", e);
            return ServiceResponse.failure("An unexpected error occurred while updating tag order.");
        }
    }
}