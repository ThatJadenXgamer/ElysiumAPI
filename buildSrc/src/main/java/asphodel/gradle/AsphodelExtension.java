package asphodel.gradle;

public class AsphodelExtension {

    private String loader;
    private String version;
    private String propertyKey;

    public String getLoader() {
        return loader;
    }

    public void setLoader(String loader) {
        this.loader = loader;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPropertyKey() {
        if (propertyKey != null && !propertyKey.isEmpty()) return propertyKey;
        if (version != null) return version.replace('.', '_').replace('-', '_');
        return null;
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }
}