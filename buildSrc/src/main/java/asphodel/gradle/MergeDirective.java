package asphodel.gradle;

public record MergeDirective(String loader, String version, String targetFqn) {

    public boolean matches(String currentLoader, String currentVersion) {
        return (loader.equals("*") || loader.equalsIgnoreCase(currentLoader)) && (version.equals("*") || version.equals(currentVersion));
    }
}