package com.allanvital.dnsao.graph;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class OverrideRegistry {

    private final Set<Object> overrideRegistry = new HashSet<>();

    public <T> void registerOverride(T module) {
        overrideRegistry.add(module);
    }

    public <T> Optional<T> getRegisteredModule(Class<T> type) {
        for (Object object : overrideRegistry) {
            if (type.isInstance(object)) {
                return Optional.of((T) object);
            }
        }
        return Optional.empty();
    }

}
