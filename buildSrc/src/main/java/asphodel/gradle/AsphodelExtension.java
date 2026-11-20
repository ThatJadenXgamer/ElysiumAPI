package asphodel.gradle;

// do not be fooled. This is not deadcode. it's actively being used for assigning metadata like the version number and loader name
// to the asphodel-loader Gradle plugin so that we can do directives on them.

@SuppressWarnings("unused")
public class AsphodelExtension {

    private String loader;
    private String version;
    private String propertyKey;

    public String getLoader() { return loader; }
    public void setLoader(String loader) { this.loader = loader; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getPropertyKey() {
        if (propertyKey != null && !propertyKey.isEmpty()) return propertyKey;
        if (version != null) return version.replace('.', '_').replace('-', '_');
        return null;
    }

    public void setPropertyKey(String propertyKey) { this.propertyKey = propertyKey; }
}