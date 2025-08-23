package com.github.chipolaris.bootforum2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.chipolaris.bootforum2.SettingsFlattener;
import com.github.chipolaris.bootforum2.config.ForumDefaultConfig;
import com.github.chipolaris.bootforum2.domain.ForumSetting;
import com.github.chipolaris.bootforum2.dto.SettingDTO;
import com.github.chipolaris.bootforum2.repository.ForumSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ForumSettingService {

    private static final Logger logger = LoggerFactory.getLogger(ForumSettingService.class);

    private final ForumSettingRepository forumSettingRepository;
    private final ForumDefaultConfig forumDefaultConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ForumSettingService(ForumSettingRepository forumSettingRepository,
                               ForumDefaultConfig forumDefaultConfig) {
        this.forumSettingRepository = forumSettingRepository;
        this.forumDefaultConfig = forumDefaultConfig;
    }

    public boolean isEmpty() {
        return forumSettingRepository.count() == 0;
    }

    /**
     * Inserts all default settings into DB in a single transaction.
     * Any exception rolls back all inserts.
     */
    @Transactional
    public void initializeFromDefaults() {
        saveAllDefaults(forumDefaultConfig);
    }

    /**
     * Inserts only missing defaults into DB, preserves existing values.
     * Executed in a single transaction.
     */
    @Transactional
    public void backfillMissingDefaults() {
        saveAllDefaults(forumDefaultConfig);
    }

    @Transactional(readOnly = false)
    protected void saveAllDefaults(ForumDefaultConfig defaults) {
        // Use a HashMap which allows null values, unlike Map.of()
        Map<String, Map<String, Object>> categories = new HashMap<>();
        categories.put("general", defaults.getGeneral());
        categories.put("users", defaults.getUsers());
        categories.put("content", defaults.getContent());
        categories.put("moderation", defaults.getModeration());
        categories.put("images", defaults.getImages());
        categories.put("attachments", defaults.getAttachments());
        categories.put("notifications", defaults.getNotifications());
        categories.put("analytics", defaults.getAnalytics());
        categories.put("system", defaults.getSystem());

        categories.forEach((category, values) -> {
            if (values != null) {
                Map<String, Object> flatValues = SettingsFlattener.flatten(values);

                flatValues.forEach((key, value) -> {
                    try {
                        String jsonValue;
                        String type;

                        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                            jsonValue = value.toString();
                            type = value.getClass().getSimpleName().toLowerCase();
                        } else {
                            jsonValue = objectMapper.writeValueAsString(value);
                            type = "json";
                        }

                        forumSettingRepository.findByCategoryAndKeyName(category, key)
                                .orElseGet(() -> {
                                    ForumSetting s = new ForumSetting();
                                    s.setCategory(category);
                                    s.setKeyName(key);
                                    s.setValue(jsonValue);
                                    s.setValueType(type);
                                    return forumSettingRepository.save(s);
                                });

                    } catch (Exception e) {
                        throw new RuntimeException("Failed to persist default setting " + key, e);
                    }
                });
            }
        });
    }

    @Transactional(readOnly = true)
    public ServiceResponse<Object> getSettingValue(String category, String key) {
        // 1. Check DB first
        Optional<ForumSetting> dbSettingOpt = forumSettingRepository.findByCategoryAndKeyName(category, key);
        if (dbSettingOpt.isPresent()) {
            ForumSetting dbSetting = dbSettingOpt.get();
            // Use the type from the DB to convert the string value back to its original object type
            return ServiceResponse.success("Found in DB", convertStringToObject(dbSetting.getValue(), dbSetting.getValueType()));
        }

        // 2. If not in DB, check default config from yml by traversing the map
        Map<String, Object> categoryDefaults = getDefaultsForCategory(category);
        if (categoryDefaults != null) {
            // This logic handles both simple keys ("allowedTypes") and nested keys ("posts.minLength")
            String[] keyParts = key.split("\\.");
            Object currentValue = categoryDefaults;
            boolean found = true;

            for (String part : keyParts) {
                if (currentValue instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentMap = (Map<String, Object>) currentValue;
                    if (currentMap.containsKey(part)) {
                        currentValue = currentMap.get(part);
                    } else {
                        found = false;
                        break;
                    }
                } else {
                    found = false;
                    break;
                }
            }

            if (found) {
                return ServiceResponse.success("Found in defaults", currentValue);
            }
        }

        logger.warn("Setting '{}.{}' not found in database or defaults.", category, key);
        return ServiceResponse.failure(String.format("Setting '%s.%s' not found.", category, key));
    }

    private Map<String, Object> getDefaultsForCategory(String category) {
        return switch (category) {
            case "general" -> forumDefaultConfig.getGeneral();
            case "users" -> forumDefaultConfig.getUsers();
            case "content" -> forumDefaultConfig.getContent();
            case "moderation" -> forumDefaultConfig.getModeration();
            case "images" -> forumDefaultConfig.getImages();
            case "attachments" -> forumDefaultConfig.getAttachments();
            case "notifications" -> forumDefaultConfig.getNotifications();
            case "analytics" -> forumDefaultConfig.getAnalytics();
            case "system" -> forumDefaultConfig.getSystem();
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public ServiceResponse<Map<String, List<SettingDTO>>> getAllSettings() {

        // 1. Get all default settings, flattened.
        Map<String, Map<String, Object>> defaultCategories = new LinkedHashMap<>();
        defaultCategories.put("general", SettingsFlattener.flatten(forumDefaultConfig.getGeneral()));
        defaultCategories.put("users", SettingsFlattener.flatten(forumDefaultConfig.getUsers()));
        defaultCategories.put("content", SettingsFlattener.flatten(forumDefaultConfig.getContent()));
        defaultCategories.put("moderation", SettingsFlattener.flatten(forumDefaultConfig.getModeration()));
        defaultCategories.put("images", SettingsFlattener.flatten(forumDefaultConfig.getImages()));
        defaultCategories.put("attachments", SettingsFlattener.flatten(forumDefaultConfig.getAttachments()));
        defaultCategories.put("notifications", SettingsFlattener.flatten(forumDefaultConfig.getNotifications()));
        defaultCategories.put("analytics", SettingsFlattener.flatten(forumDefaultConfig.getAnalytics()));
        defaultCategories.put("system", SettingsFlattener.flatten(forumDefaultConfig.getSystem()));

        // 2. Create a map of DTOs from the flattened defaults.
        Map<String, Map<String, SettingDTO>> resultDtoMap = new LinkedHashMap<>();
        defaultCategories.forEach((category, flatValues) -> {
            if (flatValues != null) {
                Map<String, SettingDTO> settings = new LinkedHashMap<>();
                flatValues.forEach((key, value) -> {
                    settings.put(key, new SettingDTO(
                            key,
                            prettifyKey(key),
                            guessType(value),
                            value,
                            guessOptions(key)
                    ));
                });
                resultDtoMap.put(category, settings);
            }
        });

        // 3. Get all DB settings and override the defaults.
        List<ForumSetting> dbSettings = forumSettingRepository.findAll();
        for (ForumSetting dbSetting : dbSettings) {
            if (resultDtoMap.containsKey(dbSetting.getCategory())) {
                Map<String, SettingDTO> categorySettings = resultDtoMap.get(dbSetting.getCategory());
                if (categorySettings.containsKey(dbSetting.getKeyName())) {
                    SettingDTO dto = categorySettings.get(dbSetting.getKeyName());
                    Object dbValue = convertStringToObject(dbSetting.getValue(), dto.type());
                    // Create a new DTO with the updated value from DB
                    SettingDTO updatedDto = new SettingDTO(dto.key(), dto.label(), dto.type(), dbValue, dto.options());
                    categorySettings.put(dbSetting.getKeyName(), updatedDto);
                }
            }
        }

        // 4. Convert the map of maps of DTOs to the final structure Map<String, List<SettingDTO>>
        Map<String, List<SettingDTO>> finalResult = new LinkedHashMap<>();
        resultDtoMap.forEach((category, settingsMap) -> {
            finalResult.put(category, new ArrayList<>(settingsMap.values()));
        });

        return ServiceResponse.success("Settings fetched successfully", finalResult);
    }

    private Object convertStringToObject(String value, String type) {
        if (value == null) return null;
        try {
            return switch (type) {
                case "boolean" -> Boolean.parseBoolean(value);
                case "number" -> objectMapper.readValue(value, Number.class);
                case "list", "json" -> objectMapper.readValue(value, Object.class);
                default -> value; // It's a string
            };
        } catch (Exception e) {
            logger.warn("Could not parse setting value '{}' of type '{}'. Returning as string.", value, type, e);
            return value;
        }
    }

    private String prettifyKey(String key) {
        // Add space before capital letters (for camelCase) and capitalize first letter
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        String finalString = spaced.replace(".", " / ").replace("_", " ");
        return finalString.substring(0, 1).toUpperCase() + finalString.substring(1);
    }

    private String guessType(Object val) {
        if (val instanceof Boolean) return "boolean";
        if (val instanceof Number) return "number";
        if (val instanceof Collection) return "list";
        if (val instanceof Map) return "json";
        return "string";
    }

    private List<String> guessOptions(String key) {
        return switch (key) {
            case "general.theme.primaryColor" -> List.of("#0044cc", "#ff6600", "#00cc44", "#8a2be2");
            case "users.registration.type" -> List.of("open", "invite", "approval");
            case "attachments.storage" -> List.of("local", "s3", "cdn");
            case "notifications.digest.frequency" -> List.of("daily", "weekly", "monthly");
            case "system.caching" -> List.of("local", "redis", "memcached");
            case "system.searchEngine" -> List.of("lucene", "elasticsearch", "solr");
            default -> List.of();
        };
    }

    @Transactional(readOnly = false)
    public ServiceResponse<Void> saveOrUpdate(Map<String, List<SettingDTO>> settings) {
        for (var entry : settings.entrySet()) {
            String category = entry.getKey();
            for (SettingDTO dto : entry.getValue()) {
                try {
                    ForumSetting existing = forumSettingRepository.findByCategoryAndKeyName(category, dto.key())
                            .orElseGet(() -> ForumSetting.newInstance(category, dto.key()));

                    String valueToSave;
                    if (dto.value() instanceof String || dto.value() instanceof Number || dto.value() instanceof Boolean) {
                        valueToSave = dto.value().toString();
                    } else {
                        // It's a list or object, serialize to JSON
                        valueToSave = objectMapper.writeValueAsString(dto.value());
                    }

                    existing.setValue(valueToSave);
                    existing.setValueType(dto.type());
                    forumSettingRepository.save(existing);
                } catch (Exception e) {
                    logger.error("Failed to save setting {}/{}", category, dto.key(), e);
                    return ServiceResponse.failure("Failed to save setting " + dto.key());
                }
            }
        }
        return ServiceResponse.success("Settings saved successfully");
    }
}