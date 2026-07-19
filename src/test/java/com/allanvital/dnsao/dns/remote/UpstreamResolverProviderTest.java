package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;
import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpstreamResolverProviderTest {

    private final UpstreamResolver quad9 = new NamedResolver("quad9");
    private final UpstreamResolver cloudflare = new NamedResolver("cloudflare");
    private final UpstreamResolver google = new NamedResolver("google");
    private final List<UpstreamResolver> allResolvers = List.of(quad9, cloudflare, google);
    private UpstreamResolverProvider provider;

    @BeforeEach
    public void setup() {
        UpstreamResolverBuilder resolverBuilder = new UpstreamResolverBuilder(null, List.of());
        resolverBuilder.setResolvers(allResolvers);
        resolverBuilder.setNamedResolvers(Map.of(
                "quad9", quad9,
                "cloudflare", cloudflare,
                "google", google
        ));

        GroupInnerConf main = new GroupInnerConf();
        main.setUpstreams(List.of("quad9"));
        GroupInnerConf kids = new GroupInnerConf();
        kids.setUpstreams(List.of("cloudflare", "google"));
        GroupInnerConf broken = new GroupInnerConf();
        broken.setUpstreams(List.of("cloudflare", "unknown"));

        provider = new UpstreamResolverProvider(resolverBuilder, 10, Map.of(
                "main", main,
                "kids", kids,
                "broken", broken
        ));
    }

    @Test
    public void usesGroupUpstreamsWhenAllNamesAreKnown() {
        assertEquals(List.of(cloudflare, google), provider.getResolversToUse(UpstreamRoutingPolicy.forGroup("kids")));
    }

    @Test
    public void usesMainUpstreamsWhenGroupHasNoSpecificUpstreams() {
        assertEquals(List.of(quad9), provider.getResolversToUse(UpstreamRoutingPolicy.forGroup("unconfigured")));
    }

    @Test
    public void usesWholeResolverPoolWhenGroupReferencesUnknownUpstreamName() {
        assertEquals(allResolvers, provider.getResolversToUse(UpstreamRoutingPolicy.forGroup("broken")));
    }

    @Test
    public void winnerPrioritizationIsScopedToRoutingPolicy() {
        UpstreamResolver mainPrimary = new NamedResolver("main-primary");
        UpstreamResolver shared = new NamedResolver("shared");
        UpstreamResolver kidsPrimary = new NamedResolver("kids-primary");
        UpstreamResolverBuilder resolverBuilder = new UpstreamResolverBuilder(null, List.of());
        resolverBuilder.setResolvers(List.of(mainPrimary, shared, kidsPrimary));
        resolverBuilder.setNamedResolvers(Map.of(
                "main-primary", mainPrimary,
                "shared", shared,
                "kids-primary", kidsPrimary
        ));

        GroupInnerConf main = new GroupInnerConf();
        main.setUpstreams(List.of("main-primary", "shared"));
        GroupInnerConf kids = new GroupInnerConf();
        kids.setUpstreams(List.of("kids-primary", "shared"));
        UpstreamResolverProvider scopedProvider = new UpstreamResolverProvider(resolverBuilder, 2, Map.of(
                "main", main,
                "kids", kids
        ));

        scopedProvider.notifyLastWinner(shared, UpstreamRoutingPolicy.forGroup("kids"));

        assertEquals(List.of(shared, kidsPrimary), scopedProvider.getResolversToUse(UpstreamRoutingPolicy.forGroup("kids")));
        assertEquals(List.of(mainPrimary, shared), scopedProvider.getResolversToUse(UpstreamRoutingPolicy.forGroup("main")));
    }

    @Test
    public void singleResolverSelectionShouldRespectRoutingPolicy() {
        assertEquals(List.of(cloudflare), provider.getSingleResolverToUse(UpstreamRoutingPolicy.forGroup("kids")));
        assertEquals(List.of(quad9), provider.getSingleResolverToUse(UpstreamRoutingPolicy.forGroup("unconfigured")));
    }

    @Test
    public void singleResolverSelectionShouldPreferPolicyWinner() {
        provider.notifyLastWinner(google, UpstreamRoutingPolicy.forGroup("kids"));

        assertEquals(List.of(google), provider.getSingleResolverToUse(UpstreamRoutingPolicy.forGroup("kids")));
    }

    @Test
    public void nullRoutingPolicyShouldUpdateGlobalWinner() {
        provider.notifyLastWinner(google, null);

        assertEquals(List.of(google, quad9, cloudflare), provider.getResolversToUse(null));
        assertEquals(List.of(google), provider.getSingleResolverToUse(null));
    }

    private record NamedResolver(String name) implements UpstreamResolver {
        @Override
        public String getIp() {
            return name;
        }

        @Override
        public int getPort() {
            return 53;
        }

        @Override
        public Message send(Message query) throws IOException {
            return null;
        }
    }
}
