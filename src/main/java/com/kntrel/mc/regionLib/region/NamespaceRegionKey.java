package com.kntrel.mc.regionLib.region;

import com.kntrel.mc.regionLib.RegionLib;
import com.kntrel.mc.regionLib.region.context.RegionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifies a region using a namespace and either a name or numeric id.
 */
public record NamespaceRegionKey(@NotNull String namespace, @Nullable String name, long id) {

    //Factory
    /**
     * Parses a raw key string into a {@link NamespaceRegionKey}.
     *
     * @param raw raw key string (namespace:name or namespace#id)
     * @return parsed key
     */
    public static NamespaceRegionKey of(String raw) {
        int split;

        int colonPos = raw.indexOf(':');
        int hashPos = raw.indexOf('#');
        if (colonPos < 0 && hashPos < 0) {
            split = -1;
        } else if (colonPos < 0) {
            split = hashPos;
        } else if (hashPos < 0) {
            split = colonPos;
        } else {
            split = Math.min(colonPos, hashPos);
        }

        if (split < 0) {
            RegionContext defaultCtx = RegionLib.getDefaultContext();
            if (defaultCtx == null) {
                throw new IllegalStateException("No default region context is set, cannot resolve region '" + raw + "'");
            }
            try {
                return new NamespaceRegionKey(defaultCtx.getNamespace(), Long.parseLong(raw));
            } catch (NumberFormatException ignored) {}
            return new NamespaceRegionKey(defaultCtx.getNamespace(), raw);
        }

        char splitChar = raw.charAt(split);
        String namespace = raw.substring(0, split);
        if (splitChar == ':') {
            return new NamespaceRegionKey(namespace, raw.substring(split + 1));
        }
        try {
            long id = Long.parseLong(raw.substring(split + 1));
            return new NamespaceRegionKey(namespace, id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid region id in key '" + raw + "'");
        }
    }

    /**
     * Creates a namespace key, ensuring either name or id is set.
     *
     * @param namespace namespace identifier
     * @param name region name, or null if using id
     * @param id region id, or -1 if using name
     */
    public NamespaceRegionKey {
        if (name == null && id < 0) {
            throw new IllegalArgumentException("Either name or id must be provided");
        }
    }

    /**
     * Creates a name-based key.
     *
     * @param namespace namespace identifier
     * @param name region name
     */
    public NamespaceRegionKey(@NotNull String namespace, @NotNull String name) {
        this(namespace, name, -1);
    }

    /**
     * Creates an id-based key.
     *
     * @param namespace namespace identifier
     * @param id region id
     */
    public NamespaceRegionKey(@NotNull String namespace, long id) {
        this(namespace, null, id);
    }

    /**
     * Returns whether this key targets a region name.
     *
     * @return true if name is set
     */
    public boolean isByName() {
        return this.name != null;
    }

    /**
     * Returns whether this key targets a region id.
     *
     * @return true if id is set
     */
    public boolean isById() {
        return this.id >= 0;
    }

    /**
     * Resolves the region context for the namespace.
     *
     * @return region context
     */
    public RegionContext getRegionContext() {
        RegionContext out = RegionLib.getContext(this.namespace);
        if (out == null) {
            throw new IllegalStateException("No region context with namespace '" + this.namespace + "' is registered");
        }
        return out;
    }

    @Override
    public String toString() {
        if (this.isByName()) {
            return this.namespace + ":" + this.name;
        }
        return this.namespace + "#" + this.id;
    }
}
