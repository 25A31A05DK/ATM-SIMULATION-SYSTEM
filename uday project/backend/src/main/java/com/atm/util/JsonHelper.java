package com.atm.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight JSON parser and serializer with ZERO external dependencies.
 * Enables running the backend on bare Java without requiring Jackson or Gson jars.
 */
public class JsonHelper {

    /**
     * Parses simple JSON object string into Map<String, String>.
     * Handles string, number, and boolean values.
     */
    public static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }

        // Match "key"\s*:\s*("value"|true|false|[0-9.]+)
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,\\}\\s]+))");
        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (value != null) {
                // Remove potential quotes or whitespace
                value = value.trim();
                map.put(key, value);
            }
        }
        return map;
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String successResponse(String message, String extraJsonFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"message\":\"").append(escape(message)).append("\"");
        if (extraJsonFields != null && !extraJsonFields.trim().isEmpty()) {
            sb.append(",").append(extraJsonFields);
        }
        sb.append("}");
        return sb.toString();
    }

    public static String errorResponse(String errorMessage) {
        return "{\"success\":false,\"message\":\"" + escape(errorMessage) + "\"}";
    }
}
