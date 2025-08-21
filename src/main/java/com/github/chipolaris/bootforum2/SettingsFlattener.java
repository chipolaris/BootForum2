package com.github.chipolaris.bootforum2;

import java.util.*;

public class SettingsFlattener {

    /**
     * Recursively flatten nested maps into dot-separated keys.
     * Example:
     * { "registration": { "type": "open", "captchaEnabled": true } }
     * =>
     * { "registration.type": "open", "registration.captchaEnabled": true }
     */
    public static Map<String, Object> flatten(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenRecursive("", source, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void flattenRecursive(String prefix, Map<String, Object> current, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : current.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenRecursive(key, (Map<String, Object>) value, result);
            } else {
                result.put(key, value);
            }
        }
    }
}
