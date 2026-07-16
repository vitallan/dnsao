package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryOutcome;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SimpleAuthorityQueryClientTest {

    private AuthorityEndpoint authorityEndpoint;
    private Message query;

    @BeforeEach
    public void setup() throws Exception {
        authorityEndpoint = new AuthorityEndpoint("ns1.example.com.", InetAddress.getLoopbackAddress(), 53);
        query = MessageHelper.buildARequest("example.com");
    }

    @Test
    public void shouldUseTcpFallbackWhenUdpResponseIsTruncated() throws Exception {
        Message truncated = MessageHelper.buildAResponse(query, "192.0.2.10", 300);
        truncated.getHeader().setFlag(Flags.TC);
        Message full = MessageHelper.buildAResponse(query, "93.184.216.34", 300);

        RecordingResolverFactory resolverFactory = new RecordingResolverFactory(List.of(
                new FakeSimpleResolver(truncated, null),
                new FakeSimpleResolver(full, null)
        ));

        SimpleAuthorityQueryClient client = new SimpleAuthorityQueryClient(resolverFactory);

        AuthorityQueryResult result = client.query(authorityEndpoint, query);

        assertEquals(AuthorityQueryOutcome.SUCCESS, result.getOutcome());
        assertEquals("93.184.216.34", com.allanvital.dnsao.graph.bean.MessageHelper.extractIpFromResponseMessage(result.getResponse()));
        assertEquals(List.of(false, true), resolverFactory.getTcpModes());
    }

    @Test
    public void shouldNotUseTcpWhenUdpResponseIsComplete() throws Exception {
        Message full = MessageHelper.buildAResponse(query, "93.184.216.34", 300);

        RecordingResolverFactory resolverFactory = new RecordingResolverFactory(List.of(
                new FakeSimpleResolver(full, null)
        ));

        SimpleAuthorityQueryClient client = new SimpleAuthorityQueryClient(resolverFactory);

        AuthorityQueryResult result = client.query(authorityEndpoint, query);

        assertEquals(AuthorityQueryOutcome.SUCCESS, result.getOutcome());
        assertEquals(List.of(false), resolverFactory.getTcpModes());
    }

    @Test
    public void shouldReturnErrorWhenTcpFallbackFails() throws Exception {
        Message truncated = MessageHelper.buildAResponse(query, "192.0.2.10", 300);
        truncated.getHeader().setFlag(Flags.TC);

        RecordingResolverFactory resolverFactory = new RecordingResolverFactory(List.of(
                new FakeSimpleResolver(truncated, null),
                new FakeSimpleResolver(null, new IOException("tcp_failed"))
        ));

        SimpleAuthorityQueryClient client = new SimpleAuthorityQueryClient(resolverFactory);

        AuthorityQueryResult result = client.query(authorityEndpoint, query);

        assertEquals(AuthorityQueryOutcome.ERROR, result.getOutcome());
        assertTrue(result.getError() instanceof IOException);
        assertEquals(List.of(false, true), resolverFactory.getTcpModes());
    }
}
