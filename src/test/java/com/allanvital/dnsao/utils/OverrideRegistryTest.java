package com.allanvital.dnsao.utils;

import com.allanvital.dnsao.graph.OverrideRegistry;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class OverrideRegistryTest {

    private OverrideRegistry overrideRegistry = new OverrideRegistry();

    interface Iface {

    }

    class IImpl implements Iface {

        private String name;

        public IImpl(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            IImpl i = (IImpl) o;
            return Objects.equals(name, i.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    @Test
    public void registrySimpleTests() {
        String test = "test";
        assertTrue(overrideRegistry.getRegisteredModule(String.class).isEmpty());
        assertEquals("notest", overrideRegistry.getRegisteredModule(String.class).orElse("notest"));
        overrideRegistry.registerOverride(test);
        assertFalse(overrideRegistry.getRegisteredModule(String.class).isEmpty());
    }

    @Test
    public void registryInterfaceClass() {
        Iface ifaceA = new IImpl("a");
        assertTrue(overrideRegistry.getRegisteredModule(Iface.class).isEmpty());
        assertEquals(new IImpl("b"), overrideRegistry.getRegisteredModule(Iface.class).orElse(new IImpl("b")));
        overrideRegistry.registerOverride(ifaceA);
        assertFalse(overrideRegistry.getRegisteredModule(Iface.class).isEmpty());
    }

}
