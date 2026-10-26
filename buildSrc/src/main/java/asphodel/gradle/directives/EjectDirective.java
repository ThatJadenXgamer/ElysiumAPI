package asphodel.gradle.directives;

public record EjectDirective(String loader, String version) {

    public boolean matches(String currentLoader, String currentVersion) {
        return DirectiveMatcher.matches(loader, currentLoader, false) && DirectiveMatcher.matches(version, currentVersion, true);
    }
}