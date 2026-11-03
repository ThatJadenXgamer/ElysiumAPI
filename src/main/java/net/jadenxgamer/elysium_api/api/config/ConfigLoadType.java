package net.jadenxgamer.elysium_api.api.config;

/**
 * The set of load stages a config entry can belong to.
 * <p>
 * Every {@link ElysiumConfigEntry} declares exactly one load type via
 * {@link ElysiumConfigEntry#loadType(ConfigLoadType)}. Entries of the same load type within
 * a single mod's config class are grouped together and written into a dedicated file with differing
 * behaviours based on what load type was associated with them.
 * <p>
 * The main thing load type dictates is <b>when</b> the file is read.
 * <ul>
 *      <li><code>COMMON</code> Loaded on both the physical client and physical server.
 *      <li><code>STARTUP</code> Behaves similarly to common but loads immediately upon game launch.
 *      <li><code>CLIENT</code> Loaded only on the physical client.
 *      <li><code>SERVER</code> Loaded on the physical client and server but can be overwritten by
 *      integrated or dedicated server world configs and is network synced to the clients.
 * </ul>
 * <p>
 * The {@link #fileName()} value is the stem used when producing the on-disk file. The full
 * path for a given mod is {@code <gameDir>/config/<modId>/<loadType>.css}.
 */
public enum ConfigLoadType {

    /**
     * Loaded independently on both the physical client and the physical server.
     * <p>
     * Common entries are the default when no {@code loadType} is specified. They are never
     * synced across the network, which means a multiplayer client and server may hold
     * different values. Use this for entries whose effect is naturally local, and prefer
     * {@link #SERVER} for anything that must be authoritative.
     */
    COMMON("common"),

    /**
     * Loaded only on the physical client.
     * <p>
     * Intended for visual and local-preference settings that have no meaningful effect on the server.
     */
    CLIENT("client"),

    /**
     * Loaded on the server and synced to every connected client.
     * <p>
     * Entries of this type are server-authoritative. The server pushes their values to
     * clients on join, and clients receive read-only copies that are reverted when the
     * connection ends. Servers may override the global file on a per-world basis by placing
     * a file under {@code <world>/serverconfig/<modId>/server.css}.
     */
    SERVER("server"),

    /**
     * Loaded as early as possible, before registries and other world state are initialized.
     * <p>
     * Intended for values that must be available during bootstrap, such as registry keys,
     * resource pack selections, or splash text entries. STARTUP entries are read once at
     * mod initialization and are not reloaded through the standard reload path.
     */
    STARTUP("startup");

    private final String fileName;

    ConfigLoadType(String fileName) {
        this.fileName = fileName;
    }

    /**
     * The file stem used when writing this load type to disk.
     * <p>
     * For example, {@link #COMMON} returns {@code "common"} and produces the file
     * {@code <gameDir>/config/<modId>/common.css}.
     *
     * @return the lowercase file stem
     */
    public String fileName() {
        return fileName;
    }
}