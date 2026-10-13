package asphodel.gradle.directives;

import org.gradle.api.GradleException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DirectiveMatcher {

    private static final Pattern OPERATOR_PATTERN = Pattern.compile("^(>=|<=|>|<|!)?(.*)$");

    private DirectiveMatcher() {}

    public static boolean matches(String pattern, String actualValue, boolean isVersionPattern) {
        if (pattern == null) return true;
        pattern = pattern.trim();
        if (pattern.isEmpty() || pattern.equals("*")) return true;

        Matcher opMatcher = OPERATOR_PATTERN.matcher(pattern);
        if (!opMatcher.matches()) return false;

        String operator = opMatcher.group(1) == null ? "" : opMatcher.group(1);
        String expectedValue = opMatcher.group(2) == null ? "" : opMatcher.group(2).trim();

        if (!operator.isEmpty() && expectedValue.isEmpty())
            throw new GradleException("Invalid directive pattern '" + pattern + "': operator '" + operator + "' requires a value");
        if (operator.isEmpty()) return expectedValue.equalsIgnoreCase(actualValue);
        if (operator.equals("!")) return !expectedValue.equalsIgnoreCase(actualValue);

        int result = isVersionPattern ? compareVersions(expectedValue, actualValue) : expectedValue.compareTo(actualValue);
        return switch (operator) {
            case ">" -> result < 0;
            case ">=" -> result <= 0;
            case "<" -> result > 0;
            case "<=" -> result >= 0;
            default -> false;
        };
    }

    private static int compareVersions(String firstVersion, String secondVersion) {
        int[] aSegments = parseVersionSegments(firstVersion);
        int[] bSegments = parseVersionSegments(secondVersion);
        int count = Math.max(aSegments.length, bSegments.length);
        for (int segmentIndex = 0; segmentIndex < count; segmentIndex++) {
            int aSegment = segmentIndex < aSegments.length ? aSegments[segmentIndex] : 0;
            int bSegment = segmentIndex < bSegments.length ? bSegments[segmentIndex] : 0;
            if (aSegment != bSegment) return Integer.compare(aSegment, bSegment);
        }
        return 0;
    }

    private static int[] parseVersionSegments(String version) {
        String[] segments = version.split("\\.");
        int[] numerics = new int[segments.length];
        for (int index = 0; index < segments.length; index++) numerics[index] = leadingInt(segments[index]);
        return numerics;
    }

    private static int leadingInt(String segment) {
        int end = 0;
        while (end < segment.length() && Character.isDigit(segment.charAt(end))) end++;
        if (end == 0) return 0;
        try { return Integer.parseInt(segment.substring(0, end)); } catch (NumberFormatException ex) { return 0; }
    }
}