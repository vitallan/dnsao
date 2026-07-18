package com.allanvital.dnsao.component;

import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.UpstreamRoutingPolicy;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupUpstreamRoutingTest extends TestHolder {

    private static final String DEFAULT_IP = "10.10.10.10";
    private static final String MAIN_IP = "10.10.10.20";
    private static final String KIDS_IP = "10.10.10.30";

    private QueryProcessor processor;

    @BeforeEach
    public void setup() throws Exception {
        registerOverride(new RoutingAwareResolverProvider());
        safeStart("groups/group-upstreams-routing.yml");
        QueryProcessorFactory factory = assembler.getQueryProcessorFactory();
        processor = factory.buildQueryProcessor();
    }

    @Test
    public void routesClientGroupToConfiguredUpstreamPool() throws Exception {
        DnsQuery response = executeQueryFrom("127.0.0.10", "kids.example.com");

        assertEquals(KIDS_IP, MessageHelper.extractIpFromResponseMessage(response.getResponse()));
    }

    @Test
    public void routesUngroupedClientToMainConfiguredUpstreamPool() throws Exception {
        DnsQuery response = executeQueryFrom("127.0.0.99", "main.example.com");

        assertEquals(MAIN_IP, MessageHelper.extractIpFromResponseMessage(response.getResponse()));
    }

    private DnsQuery executeQueryFrom(String clientIp, String domain) throws Exception {
        Message request = MessageHelper.buildARequest(domain);
        return processor.processExternalQuery(InetAddress.getByName(clientIp), request.toWire());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

    private static class RoutingAwareResolverProvider implements ResolverProvider {
        private final UpstreamResolver defaultResolver = new StaticIpResolver("default", DEFAULT_IP);
        private final UpstreamResolver mainResolver = new StaticIpResolver("main", MAIN_IP);
        private final UpstreamResolver kidsResolver = new StaticIpResolver("kids", KIDS_IP);

        @Override
        public List<UpstreamResolver> getAllResolvers() {
            return List.of(defaultResolver, mainResolver, kidsResolver);
        }

        @Override
        public List<UpstreamResolver> getResolversToUse() {
            return List.of(defaultResolver);
        }

        @Override
        public List<UpstreamResolver> getResolversToUse(UpstreamRoutingPolicy routingPolicy) {
            if (routingPolicy != null && "kids".equals(routingPolicy.group())) {
                return List.of(kidsResolver);
            }
            if (routingPolicy != null && "main".equals(routingPolicy.group())) {
                return List.of(mainResolver);
            }
            return List.of(defaultResolver);
        }
    }

    private record StaticIpResolver(String resolverName, String ip) implements UpstreamResolver {
        @Override
        public String getIp() {
            return resolverName;
        }

        @Override
        public int getPort() {
            return 53;
        }

        @Override
        public String name() {
            return resolverName;
        }

        @Override
        public Message send(Message query) throws IOException {
            return MessageHelper.buildAResponse(query, ip, 300);
        }
    }
}
