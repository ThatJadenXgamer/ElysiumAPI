package asphodel.gradle.directives;

public record MixinDirective(String loader, String version, String file, String env, int line) {

    public boolean matches(String currentLoader, String currentVersion) {
        return DirectiveMatcher.matches(loader, currentLoader, false) && DirectiveMatcher.matches(version, currentVersion, true);
    }
}