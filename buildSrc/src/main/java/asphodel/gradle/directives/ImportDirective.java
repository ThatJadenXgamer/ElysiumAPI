package asphodel.gradle.directives;

public record ImportDirective(String loader, String version, String targetFqn, boolean isStatic) {

    public boolean matches(String currentLoader, String currentVersion) {
        return DirectiveMatcher.matches(loader, currentLoader, false) && DirectiveMatcher.matches(version, currentVersion, true);
    }
}