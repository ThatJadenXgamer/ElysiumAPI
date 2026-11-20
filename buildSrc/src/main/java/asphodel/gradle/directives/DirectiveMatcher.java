package asphodel.gradle.directives;

import org.gradle.api.GradleException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DirectiveMatcher {

    private static final Pattern OPERATOR_PATTERN = Pattern.compile("^(>=|<=|>|<|!)?(.*)$"); // '*' acts as the default "any" rule
    private static final ConcurrentMap<String, int[]> VERSION_CACHE = new ConcurrentHashMap<>();

    private DirectiveMatcher() {}

    public static boolean matchesPair(String loaderPattern, String versionPattern, String currentLoader, String currentVersion) {
        return matches(loaderPattern, currentLoader, false) && matches(versionPattern, currentVersion, true);
    }

    public static Map<String, String> parseConditions(String input) {
        Map<String, String> result = new LinkedHashMap<>();
        if (input == null) return result;
        String normalized = input.replace("->", ","); // directives can optionally call `->` instead of `,` whenever they want
        for (String token : normalized.split(",")) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex < 0) continue;
            String key = token.substring(0, equalsIndex).trim().toLowerCase();
            String value = token.substring(equalsIndex + 1).trim();
            if (key.isEmpty() || value.isEmpty()) continue;
            result.put(key, value);
        }
        return result;
    }

    // deals with validating and matching the directive conditions supplied for version and loader; and of course also the operators for both
    public static boolean matches(String pattern, String actualValue, boolean isVersionPattern) {
        if (pattern == null) return true;
        pattern = pattern.trim();

        if (pattern.isEmpty() || pattern.equals("*")) return true; // `*` means any match
        char first = pattern.charAt(0);
        if (first != '>' && first != '<' && first != '!') return pattern.equalsIgnoreCase(actualValue); //

        Matcher opMatcher = OPERATOR_PATTERN.matcher(pattern);
        if (!opMatcher.matches()) return false;

        String operator = opMatcher.group(1) == null ? "" : opMatcher.group(1);
        String expectedValue = opMatcher.group(2) == null ? "" : opMatcher.group(2).trim();
        if (!operator.isEmpty() && expectedValue.isEmpty()) throw new GradleException("Invalid directive pattern '" + pattern + "': operator '" + operator + "' requires a value");

        if (operator.isEmpty()) return expectedValue.equalsIgnoreCase(actualValue);
        if (operator.equals("!")) return !expectedValue.equalsIgnoreCase(actualValue); // NOT
        int result = isVersionPattern ? compareVersions(expectedValue, actualValue) : expectedValue.compareTo(actualValue);
        return switch (operator) {
            case ">" -> result < 0; // MORE THAN
            case ">=" -> result <= 0; // MORE THAN OR EQUAL
            case "<" -> result > 0; // LESS THAN
            case "<=" -> result >= 0; // LESS THAN OR EQUAL
            default -> false;
        };
    }

    // this makes sure that versions with the yearly-quarter 'YY.Q' formatting are considered newer than the legacy major-minor '1.AA.B' format
    // i.e, stuff like 26.1 is treated as being newer than 1.21.1... I still think that's fucking stupid, but I digress
    // TODO: if you're really bored, look into supporting snapshot, release candidates and pre-release formatting too
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
        int[] cached = VERSION_CACHE.get(version);
        if (cached != null) return cached;
        String[] segments = version.split("\\.");
        int[] numerics = new int[segments.length];
        for (int index = 0; index < segments.length; index++) numerics[index] = leadingInt(segments[index]);
        VERSION_CACHE.putIfAbsent(version, numerics);
        return numerics;
    }

    private static int leadingInt(String segment) {
        int end = 0;
        while (end < segment.length() && Character.isDigit(segment.charAt(end))) end++;
        if (end == 0) return 0;
        try { return Integer.parseInt(segment.substring(0, end)); } catch (NumberFormatException ex) { return 0; }
    }
}