package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.CacheEntryFactory;
import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.util.concurrent.Semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmWorkerCacheabilityTest {

    @Test
    public void rewarmShouldStoreNoErrorEmptyAnswerWithSoa() throws Exception {
        CacheManager cacheManager = cacheManager();
        Message request = MessageHelper.buildARequest("example.com");
        Message cached = MessageHelper.buildAResponse(request, "93.184.216.34", 1);
        cacheManager.put("A:example.com.", cached, 1L);
        RewarmContext rewarmContext = new RewarmContext("A:example.com.", new RewarmTask("A:example.com.", -1L), cacheManager.safeGet("A:example.com."), request.getQuestion(), 0, false);
        Message refresh = buildNoErrorEmptyWithSoa(request, 90L, 180L);
        QueryProcessorFactory factory = queryProcessorFactory(refresh);

        RewarmWorker worker = new RewarmWorker(cacheManager, factory, rewarmContext, new Semaphore(1), new CacheEntryFactory());
        worker.run();

        DnsCacheEntry entry = cacheManager.safeGet("A:example.com.");
        assertNotNull(entry);
        assertEquals(Rcode.NOERROR, entry.getResponse().getRcode());
        assertEquals(0, entry.getResponse().getSection(Section.ANSWER).size());
        assertEquals(90L, entry.getConfiguredTtlInSeconds());
    }

    @Test
    public void rewarmShouldStoreNxdomainWithSoa() throws Exception {
        CacheManager cacheManager = cacheManager();
        Message request = MessageHelper.buildARequest("example.com");
        Message cached = MessageHelper.buildAResponse(request, "93.184.216.34", 1);
        cacheManager.put("A:example.com.", cached, 1L);
        RewarmContext rewarmContext = new RewarmContext("A:example.com.", new RewarmTask("A:example.com.", -1L), cacheManager.safeGet("A:example.com."), request.getQuestion(), 0, false);
        Message refresh = buildNxdomainWithSoa(request, 120L, 180L, 300L);
        QueryProcessorFactory factory = queryProcessorFactory(refresh);

        RewarmWorker worker = new RewarmWorker(cacheManager, factory, rewarmContext, new Semaphore(1), new CacheEntryFactory());
        worker.run();

        DnsCacheEntry entry = cacheManager.safeGet("A:example.com.");
        assertNotNull(entry);
        assertEquals(Rcode.NXDOMAIN, entry.getResponse().getRcode());
        assertEquals(180L, entry.getConfiguredTtlInSeconds());
    }

    private CacheManager cacheManager() {
        CacheConf cacheConf = new CacheConf();
        cacheConf.setEnabled(true);
        return new CacheManager(cacheConf, new FixedTimeRewarmScheduler(100), new ExpiredConf(), new KeepProvider(cacheConf));
    }

    private QueryProcessorFactory queryProcessorFactory(Message responseMessage) {
        return new QueryProcessorFactory(new QueryProcessorDependencies(null, null, null)) {
            @Override
            public QueryProcessor buildQueryProcessor() {
                return new QueryProcessor(new QueryProcessorDependencies(null, null, null)) {
                    @Override
                    public DnsQuery processInternalQuery(Message message) {
                        DnsQueryRequest request = new DnsQueryRequest(InetAddress.getLoopbackAddress());
                        request.setOriginalRequest(message);
                        request.setRequest(message);
                        DnsQueryResponse response = new DnsQueryResponse(request, responseMessage);
                        return new DnsQuery(request, response);
                    }

                    @Override
                    public DnsQuery processSingleUpstreamInternalQuery(Message message, com.allanvital.dnsao.dns.remote.UpstreamRoutingPolicy upstreamRoutingPolicy) {
                        return processInternalQuery(message);
                    }
                };
            }
        };
    }

    private Message buildNxdomainWithSoa(Message request, long ttl, long minimum, long soaTtl) throws Exception {
        Message response = MessageHelper.buildNxdomainResponseFrom(request, false);
        Name zone = request.getQuestion().getName();
        SOARecord soaRecord = new SOARecord(zone, DClass.IN, soaTtl, Name.fromString("ns1.example.com."), Name.fromString("hostmaster.example.com."), 1L, 3600L, 600L, 86400L, minimum);
        response.addRecord(soaRecord, Section.AUTHORITY);
        return response;
    }

    private Message buildNoErrorEmptyWithSoa(Message request, long ttl, long minimum) throws Exception {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(request.getQuestion(), Section.QUESTION);
        Name zone = request.getQuestion().getName();
        SOARecord soaRecord = new SOARecord(zone, DClass.IN, ttl, Name.fromString("ns1.example.com."), Name.fromString("hostmaster.example.com."), 1L, 3600L, 600L, 86400L, minimum);
        response.addRecord(soaRecord, Section.AUTHORITY);
        return response;
    }
}
