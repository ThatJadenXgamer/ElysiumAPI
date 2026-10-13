package asphodel.gradle.directives;

public record PatchDirective(String loader, String version, String id) {

    public boolean matches(String currentLoader, String currentVersion) {
        return DirectiveMatcher.matches(loader, currentLoader, false) && DirectiveMatcher.matches(version, currentVersion, true);
    }
}