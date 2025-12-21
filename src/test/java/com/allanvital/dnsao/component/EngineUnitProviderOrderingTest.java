package com.allanvital.dnsao.component;

import com.allanvital.dnsao.dns.processor.engine.EngineUnitProvider;
import com.allanvital.dnsao.dns.processor.engine.unit.*;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class EngineUnitProviderOrderingTest extends TestHolder {

    private EngineUnitProvider provider;

    @BeforeEach
    public void setup() throws Exception {
        safeStart("1udp-upstream-3-multiplier.yml");
        provider = queryInfraAssembler.getEngineUnitProvider();
    }

    @Test
    public void validatedEngineUnitsOrdering() {
        List<EngineUnit> engineUnits = provider.getOrderedEngineUnits();
        assertEquals(6, engineUnits.size());
        assertInstanceOf(BlockUnit.class, engineUnits.get(0));
        assertInstanceOf(LocalMappingUnit.class, engineUnits.get(1));
        assertInstanceOf(CacheUnit.class, engineUnits.get(2));
        assertInstanceOf(UpstreamUnit.class, engineUnits.get(3));
        assertInstanceOf(StaleUnit.class, engineUnits.get(4));
        assertInstanceOf(ServFailUnit.class, engineUnits.get(5));
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
    }

}
