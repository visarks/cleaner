package com.cleaner.util;

import java.nio.file.Path;

public class PathUtils {

    /**
     * Converts a glob pattern to a regex pattern.
     * Supports: *, ?, **, {}
     */
    public static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = glob.length();

        while (i < len) {
            char c = glob.charAt(i);

            switch (c) {
                case '*':
                    if (i + 1 < len && glob.charAt(i + 1) == '*') {
                        // ** matches any number of directories
                        sb.append("(?:[^/]*\\/)*");
                        i += 2;
                        // Skip following / if present
                        if (i < len && glob.charAt(i) == '/') {
                            i++;
                        }
                    } else {
                        // * matches anything except /
                        sb.append("[^/]*");
                        i++;
                    }
                    break;
                case '?':
                    sb.append("[^/]");
                    i++;
                    break;
                case '.':
                    sb.append("\\.");
                    i++;
                    break;
                case '{':
                    // Handle brace expansion {a,b,c}
                    int end = glob.indexOf('}', i);
                    if (end > i) {
                        String[] options = glob.substring(i + 1, end).split(",");
                        sb.append("(?:");
                        for (int j = 0; j < options.length; j++) {
                            if (j > 0) sb.append("|");
                            sb.append(globToRegex(options[j]));
                        }
                        sb.append(")");
                        i = end + 1;
                    } else {
                        sb.append("\\{");
                        i++;
                    }
                    break;
                case '}':
                    sb.append("\\}");
                    i++;
                    break;
                case ',':
                    sb.append("\\,");
                    i++;
                    break;
                case '\\':
                case '+':
                case '^':
                case '$':
                case '|':
                case '(':
                case ')':
                case '[':
                case ']':
                    sb.append("\\").append(c);
                    i++;
                    break;
                default:
                    sb.append(c);
                    i++;
            }
        }

        return sb.toString();
    }

    /**
     * Checks if a path matches a glob pattern.
     */
    public static boolean matches(Path path, String pattern) {
        String pathStr = path.toString().replace('\\', '/');
        String normalizedPattern = pattern.replace('\\', '/');

        // Handle patterns that match directory names (like "node_modules")
        if (!normalizedPattern.contains("*") && !normalizedPattern.contains("?") &&
            !normalizedPattern.contains("/") && !normalizedPattern.contains("\\")) {
            // Simple name match
            return pathStr.endsWith("/" + normalizedPattern) ||
                   pathStr.equals(normalizedPattern) ||
                   path.getFileName().toString().equals(normalizedPattern);
        }

        // Handle **/pattern (match at any level)
        if (normalizedPattern.startsWith("**/")) {
            String subPattern = normalizedPattern.substring(3);
            return pathStr.endsWith(subPattern) ||
                   path.getFileName().toString().matches(globToRegex(subPattern));
        }

        // Handle pattern/** (match directory and all contents)
        if (normalizedPattern.endsWith("/**")) {
            String prefix = normalizedPattern.substring(0, normalizedPattern.length() - 3);
            return pathStr.startsWith(prefix + "/") || pathStr.equals(prefix);
        }

        // Standard glob matching
        String regex = globToRegex(normalizedPattern);

        // Try full path match
        if (pathStr.matches(regex)) return true;

        // Try filename match
        String fileName = path.getFileName().toString();
        if (fileName.matches(regex)) return true;

        // Try if pattern should match anywhere in path
        return pathStr.matches(".*" + regex + ".*") || pathStr.matches(".*" + regex);
    }
}