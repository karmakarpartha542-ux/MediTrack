package com.meditrack.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lightweight JSON utility for serialization and deserialization
 * using standard Java SE String operations without external dependencies.
 */
public class JsonUtils {

    private static final Random RANDOM = new Random();

    /**
     * Escapes special characters in a string for safe JSON embedding.
     *
     * @param s raw string
     * @return JSON-escaped string
     */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Generates a unique business identifier with a prefix, 4-digit random number,
     * and timestamp suffix.
     *
     * @param prefix identifier category (e.g., "PAT", "DOC", "RM", "APT", "BIL")
     * @return generated ID string
     */
    public static String generateId(String prefix) {
        String p = (prefix != null && !prefix.trim().isEmpty()) ? prefix.trim().toUpperCase() : "ID";
        int randomFour = 1000 + RANDOM.nextInt(9000);
        long timeSuffix = System.currentTimeMillis() % 100000;
        return p + "-" + randomFour + "-" + timeSuffix;
    }

    /**
     * Extracts a string property value associated with {@code key} from a JSON object.
     * Returns empty string if the key is not present or value is null.
     *
     * @param json source JSON string
     * @param key  property key to look up
     * @return extracted string or empty string
     */
    public static String getString(String json, String key) {
        if (json == null || key == null) {
            return "";
        }

        String target = "\"" + key + "\"";
        int searchIndex = 0;

        while (searchIndex < json.length()) {
            int keyIndex = json.indexOf(target, searchIndex);
            if (keyIndex == -1) {
                return "";
            }

            int colonIndex = json.indexOf(':', keyIndex + target.length());
            if (colonIndex == -1) {
                return "";
            }

            // Ensure only whitespace exists between key and colon
            String between = json.substring(keyIndex + target.length(), colonIndex).trim();
            if (!between.isEmpty()) {
                searchIndex = keyIndex + target.length();
                continue;
            }

            // Skip whitespace after colon
            int valStart = colonIndex + 1;
            while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) {
                valStart++;
            }
            if (valStart >= json.length()) {
                return "";
            }

            char firstChar = json.charAt(valStart);

            if (firstChar == '"') {
                // Quoted string
                StringBuilder val = new StringBuilder();
                boolean escaping = false;
                for (int i = valStart + 1; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (escaping) {
                        switch (c) {
                            case '"' -> val.append('"');
                            case '\\' -> val.append('\\');
                            case 'n' -> val.append('\n');
                            case 'r' -> val.append('\r');
                            case 't' -> val.append('\t');
                            case 'b' -> val.append('\b');
                            case 'f' -> val.append('\f');
                            default -> val.append(c);
                        }
                        escaping = false;
                    } else if (c == '\\') {
                        escaping = true;
                    } else if (c == '"') {
                        return val.toString();
                    } else {
                        val.append(c);
                    }
                }
                return val.toString();
            } else {
                // Unquoted primitive (number, boolean, null)
                int end = valStart;
                while (end < json.length()) {
                    char c = json.charAt(end);
                    if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                        break;
                    }
                    end++;
                }
                String result = json.substring(valStart, end).trim();
                return "null".equalsIgnoreCase(result) ? "" : result;
            }
        }

        return "";
    }

    /**
     * Extracts an integer property value from JSON string.
     *
     * @param json source JSON string
     * @param key  property key
     * @return parsed int or 0 on error
     */
    public static int getInt(String json, String key) {
        String val = getString(json, key);
        if (val.isEmpty()) {
            return 0;
        }
        try {
            if (val.contains(".")) {
                return (int) Double.parseDouble(val);
            }
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Extracts a double property value from JSON string.
     *
     * @param json source JSON string
     * @param key  property key
     * @return parsed double or 0.0 on error
     */
    public static double getDouble(String json, String key) {
        String val = getString(json, key);
        if (val.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Extracts a boolean property value from JSON string.
     *
     * @param json source JSON string
     * @param key  property key
     * @return parsed boolean or false on error
     */
    public static boolean getBoolean(String json, String key) {
        String val = getString(json, key);
        return "true".equalsIgnoreCase(val);
    }

    /**
     * Extracts an array of JSON object strings associated with {@code key}.
     *
     * @param json source JSON string
     * @param key  array property name
     * @return list of JSON object strings
     */
    public static List<String> getArray(String json, String key) {
        List<String> result = new ArrayList<>();
        if (json == null || key == null) {
            return result;
        }

        String target = "\"" + key + "\"";
        int keyIndex = json.indexOf(target);
        if (keyIndex == -1) {
            return result;
        }

        int colonIndex = json.indexOf(':', keyIndex + target.length());
        if (colonIndex == -1) {
            return result;
        }

        int openBracket = json.indexOf('[', colonIndex);
        if (openBracket == -1) {
            return result;
        }

        // Find matching closing bracket
        int closeBracket = -1;
        int depth = 0;
        boolean inStr = false;
        boolean escaping = false;

        for (int i = openBracket; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                inStr = !inStr;
                continue;
            }
            if (!inStr) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        closeBracket = i;
                        break;
                    }
                }
            }
        }

        if (closeBracket == -1) {
            return result;
        }

        String arrayContent = json.substring(openBracket, closeBracket + 1);
        return parseJsonArray(arrayContent);
    }

    /**
     * Parses a top-level JSON array string (e.g. {@code [{...},{...}]}) into
     * individual JSON object strings.
     *
     * @param arrayJson JSON array string
     * @return list of JSON object strings
     */
    public static List<String> parseJsonArray(String arrayJson) {
        List<String> items = new ArrayList<>();
        if (arrayJson == null || arrayJson.trim().isEmpty()) {
            return items;
        }

        String trimmed = arrayJson.trim();
        int firstBracket = trimmed.indexOf('[');
        int lastBracket = trimmed.lastIndexOf(']');

        if (firstBracket == -1 || lastBracket == -1 || lastBracket <= firstBracket) {
            return items;
        }

        String inner = trimmed.substring(firstBracket + 1, lastBracket).trim();
        if (inner.isEmpty()) {
            return items;
        }

        int depth = 0;
        int startPos = -1;
        boolean inStr = false;
        boolean escaping = false;

        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                inStr = !inStr;
                continue;
            }
            if (!inStr) {
                if (c == '{') {
                    if (depth == 0) {
                        startPos = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && startPos != -1) {
                        String obj = inner.substring(startPos, i + 1).trim();
                        if (!obj.isEmpty()) {
                            items.add(obj);
                        }
                        startPos = -1;
                    }
                }
            }
        }

        return items;
    }
}
