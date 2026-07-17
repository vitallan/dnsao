package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.cache.CacheEntryFactory;
import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CachePostHandlerWithFactoryTest {

    @Test
    public void postHandlerShouldCacheDirectPositiveAnswer() throws Exception {
        CacheManager cacheManager = cacheManager();
        CachePostHandler cachePostHandler = new CachePostHandler(cacheManager, new CacheEntryFactory());
        DnsQueryRequest request = request("example.com");
        DnsQueryResponse response = response(request, MessageHelper.buildAResponse(request.getRequest(), "93.184.216.34", 300));

        cachePostHandler.handle(request, response);

        assertNotNull(cacheManager.safeGet("A:example.com."));
    }

    @Test
    public void postHandlerShouldCacheNxdomainWithSoa() throws Exception {
        CacheManager cacheManager = cacheManager();
        CachePostHandler cachePostHandler = new CachePostHandler(cacheManager, new CacheEntryFactory());
        DnsQueryRequest request = request("example.com");
        DnsQueryResponse response = response(request, buildNxdomainWithSoa(request.getRequest(), 120L, 180L, 300L));

        cachePostHandler.handle(request, response);

        assertNotNull(cacheManager.safeGet("A:example.com."));
    }

    @Test
    public void postHandlerShouldCacheNoErrorEmptyAnswerWithSoa() throws Exception {
        CacheManager cacheManager = cacheManager();
        CachePostHandler cachePostHandler = new CachePostHandler(cacheManager, new CacheEntryFactory());
        DnsQueryRequest request = request("example.com");
        DnsQueryResponse response = response(request, buildNoErrorEmptyWithSoa(request.getRequest(), 90L, 180L));

        cachePostHandler.handle(request, response);

        assertNotNull(cacheManager.safeGet("A:example.com."));
    }

    private CacheManager cacheManager() {
        CacheConf cacheConf = new CacheConf();
        cacheConf.setEnabled(true);
        return new CacheManager(cacheConf, new FixedTimeRewarmScheduler(100), new ExpiredConf(), new KeepProvider(cacheConf));
    }

    private DnsQueryRequest request(String domain) throws Exception {
        Message message = MessageHelper.buildARequest(domain);
        DnsQueryRequest request = new DnsQueryRequest(InetAddress.getLoopbackAddress());
        request.setOriginalRequest(message);
        request.setRequest(message);
        request.setIsLocalQuery(false);
        return request;
    }

    private DnsQueryResponse response(DnsQueryRequest request, Message message) {
        DnsQueryResponse response = new DnsQueryResponse(request, message);
        response.setQueryResolvedBy(QueryResolvedBy.UPSTREAM);
        return response;
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
