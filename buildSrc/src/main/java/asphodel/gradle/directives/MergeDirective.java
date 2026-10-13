package asphodel.gradle.directives;

public record MergeDirective(String loader, String version, String targetFqn) {

    public boolean matches(String currentLoader, String currentVersion) {
        return DirectiveMatcher.matches(loader, currentLoader, false) && DirectiveMatcher.matches(version, currentVersion, true);
    }
}