package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.conf.inner.RecursiveConf;
import com.allanvital.dnsao.dns.processor.engine.unit.RecursiveUnit;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveInfraAssembler {

    protected final OverrideRegistry overrideRegistry;

    public RecursiveInfraAssembler(OverrideRegistry overrideRegistry) {
        this.overrideRegistry = overrideRegistry;
    }

    public RecursiveUnit recursiveUnit() {
        return overrideRegistry.getRegisteredModule(RecursiveUnit.class)
                .orElse(new RecursiveUnit(recursiveSessionFactory(), rootHintsProvider()));
    }

    protected RootHintsProvider rootHintsProvider() {
        return overrideRegistry.getRegisteredModule(RootHintsProvider.class)
                .orElse(new StaticRootHintsProvider());
    }

    protected AuthorityQueryClient authorityQueryClient() {
        return overrideRegistry.getRegisteredModule(AuthorityQueryClient.class)
                .orElse(new SimpleAuthorityQueryClient());
    }

    protected ReferralInterpreter referralInterpreter() {
        return overrideRegistry.getRegisteredModule(ReferralInterpreter.class)
                .orElse(new ReferralInterpreter());
    }

    protected RecursiveSessionFactory recursiveSessionFactory() {
        return overrideRegistry.getRegisteredModule(RecursiveSessionFactory.class)
                .orElse(new RecursiveSessionFactory(authorityQueryClient(), referralInterpreter(), new MinimizedQuestionProvider(), new RecursiveInternalRequestFactory(), recursiveConf()));
    }

    protected RecursiveConf recursiveConf() {
        return overrideRegistry.getRegisteredModule(RecursiveConf.class)
                .orElse(new RecursiveConf());
    }
}
