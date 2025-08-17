package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.TagCreateDTO;
import com.github.chipolaris.bootforum2.dto.TagDTO;
import com.github.chipolaris.bootforum2.dto.TagUpdateDTO;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import com.github.chipolaris.bootforum2.service.TagService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TagController {

    private static final Logger logger = LoggerFactory.getLogger(TagController.class);

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/public/tags/all")
    public ApiResponse<?> getAllTags() {
        ServiceResponse<List<TagDTO>> response = tagService.getAllTags();

        if(response.isSuccess()) {
            logger.info("Successfully fetched all tags");
            return ApiResponse.success(response.getDataObject());
        } else {
            logger.error("Error fetching all tags: {}", response.getMessages());
            return ApiResponse.error(response.getMessages(), "Failed to fetch tags.");
        }
    }

    @PostMapping("/admin/tags/create")
    public ApiResponse<?> createTag(@Valid @RequestBody TagCreateDTO tagCreateDTO) {
        logger.info("Request to create tag: {}", tagCreateDTO.label());
        ServiceResponse<TagDTO> response = tagService.createTag(tagCreateDTO);

        if(response.isSuccess()) {
            logger.info("Successfully created tag: {}", response.getDataObject());
            return ApiResponse.success(response.getDataObject());
        } else {
            logger.error("Error creating tag: {}", response.getMessages());
            return ApiResponse.error(response.getMessages(), "Failed to create tag.");
        }
    }

    @PutMapping("/admin/tags/{id}")
    public ApiResponse<?> updateTag(@PathVariable Long id, @Valid @RequestBody TagUpdateDTO tagUpdateDTO) {
        logger.info("Request to update tag with ID: {}", id);
        if (!id.equals(tagUpdateDTO.id())) {
            return ApiResponse.error("Path ID does not match payload ID.");
        }
        ServiceResponse<TagDTO> response = tagService.updateTag(tagUpdateDTO);

        if(response.isSuccess()) {
            logger.info("Successfully updated tag");
            return ApiResponse.success(response.getDataObject());
        }
        else {
            logger.error("Error updating tag: {}", response.getMessages());
            return ApiResponse.error(response.getMessages(), "Failed to update tag.");
        }
    }

    @DeleteMapping("/admin/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        logger.info("Request to delete tag with ID: {}", id);
        ServiceResponse<Void> response = tagService.deleteTag(id);

        if(response.isSuccess()) {
            logger.info("Successfully deleted tag");
            return ApiResponse.success(response.getDataObject());
        }
        else {
            logger.error("Error deleting tag: {}", response.getMessages());
            return ApiResponse.error(response.getMessages(), "Failed to delete tag.");
        }
    }

    @PutMapping("/admin/tags/reorder")
    public ApiResponse<Void> updateTagOrder(@RequestBody List<Long> orderedIds) {
        logger.info("Request to update tag order");
        ServiceResponse<Void> response = tagService.updateTagOrder(orderedIds);

        if(response.isSuccess()) {
            logger.info("Successfully updated tag order");
            return ApiResponse.success(response.getDataObject());
        }
        else {
            logger.error("Error updating tag order");
            return ApiResponse.error(response.getMessages(), "Failed to update tag order.");
        }
    }
}