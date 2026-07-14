package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.unit.RecursiveUnit;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean.CountingRootHintsProvider;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean.RecordingRecursiveSessionFactory;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean.StubRecursiveSession;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Rcode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveUnitWiringTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private List<AuthorityEndpoint> rootHints;
    private CountingRootHintsProvider rootHintsProvider;
    private RecordingRecursiveSessionFactory sessionFactory;
    private RecursiveUnit recursiveUnit;

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest("example.com");
        rootHints = List.of(endpoint("a.root-servers.net.", "198.41.0.4", 53));
        rootHintsProvider = new CountingRootHintsProvider(rootHints);
        sessionFactory = new RecordingRecursiveSessionFactory(
                new StubRecursiveSession(request, RecursiveResult.servfail(MessageHelper.buildServfailFrom(request.getRequest()), "stub"))
        );
        recursiveUnit = buildRecursiveUnit(rootHintsProvider, sessionFactory);
    }

    @Test
    public void processUsesRootHintsProviderAndSessionFactory() {
        DnsQueryResponse response = recursiveUnit.process(request);

        assertEquals(1, rootHintsProvider.getCalls());
        assertSame(request, sessionFactory.getSeenRequest());
        assertEquals(rootHints, sessionFactory.getSeenRootHints());
        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getResponse().getRcode());
    }
}
