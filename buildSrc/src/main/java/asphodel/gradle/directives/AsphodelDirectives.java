
package asphodel.gradle.directives;

public final class AsphodelDirectives {

    private AsphodelDirectives() {}

    // contains common calls in all directives
    public interface Directive {

        String loader(); // used to check what loader the assembly is being run from at the moment

        String version(); // ditto, but for the version

        default boolean matches(String currentLoader, String currentVersion) {
            return DirectiveMatcher.matchesPair(loader(), version(), currentLoader, currentVersion);
        }
    }

    // when conditions are met, forcefully ejects the file, and it does not process it on assembly
    public record EjectDirective(String loader, String version) implements Directive {}

    // allows manipulation of import lines, such as conditionally adding or removing with support for static calls
    public record ImportDirective(String loader, String version, String targetFqn, boolean isStatic) implements Directive {}

    // merges a contributor class' members to the class this directive was called at, with the contributor's implementation taking priority
    public record MergeDirective(String loader, String version, String targetFqn) implements Directive { }

    // autopopulates a mixin to the specified config JSON within the correct environment list when conditions are met
    public record MixinDirective(String loader, String version, String file, String env, int line) implements Directive {}

    // create a patch tag and using the tag's named identifier, you do operations for adding or removing lines of code
    public record PatchDirective(String loader, String version, String id) implements Directive {}
}