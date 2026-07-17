package com.allanvital.dnsao.cache;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheEntryFactoryTest {

    private final CacheEntryFactory cacheEntryFactory = new CacheEntryFactory();

    @Test
    public void shouldCreateEntryForDirectPositiveAnswer() {
        Message request = MessageHelper.buildARequest("example.com");
        Message response = MessageHelper.buildAResponse(request, "93.184.216.34", 300);

        CacheEntryCandidate result = cacheEntryFactory.build(response);

        assertTrue(result.isCacheable());
        assertNotNull(result.getDnsCacheEntry());
        assertEquals(300L, result.getDnsCacheEntry().getConfiguredTtlInSeconds());
    }

    @Test
    public void shouldCreateEntryForNxdomainWithSoa() throws Exception {
        Message response = buildNxdomainWithSoa("example.com", 120L, 180L, 300L);

        CacheEntryCandidate result = cacheEntryFactory.build(response);

        assertTrue(result.isCacheable());
        assertNotNull(result.getDnsCacheEntry());
        assertEquals(180L, result.getDnsCacheEntry().getConfiguredTtlInSeconds());
    }

    @Test
    public void shouldCreateEntryForNoErrorEmptyAnswerWithSoa() throws Exception {
        Message response = buildNoErrorEmptyWithSoa("example.com", 90L, 180L);

        CacheEntryCandidate result = cacheEntryFactory.build(response);

        assertTrue(result.isCacheable());
        assertNotNull(result.getDnsCacheEntry());
        assertEquals(90L, result.getDnsCacheEntry().getConfiguredTtlInSeconds());
    }

    @Test
    public void shouldRejectResponseWithoutPositiveOrNegativeTtlSignal() {
        Message response = new Message();
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(Rcode.NOERROR);

        CacheEntryCandidate result = cacheEntryFactory.build(response);

        assertFalse(result.isCacheable());
        assertNull(result.getDnsCacheEntry());
    }

    private Message buildNxdomainWithSoa(String domain, long ttl, long minimum, long soaTtl) throws Exception {
        Message request = MessageHelper.buildARequest(domain);
        Message response = MessageHelper.buildNxdomainResponseFrom(request, false);
        Name zone = Name.fromString(domain + ".");
        SOARecord soaRecord = new SOARecord(zone, DClass.IN, soaTtl, Name.fromString("ns1.example.com."), Name.fromString("hostmaster.example.com."), 1L, 3600L, 600L, 86400L, minimum);
        response.addRecord(soaRecord, Section.AUTHORITY);
        return response;
    }

    private Message buildNoErrorEmptyWithSoa(String domain, long ttl, long minimum) throws Exception {
        Message request = MessageHelper.buildARequest(domain);
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(request.getQuestion(), Section.QUESTION);
        Name zone = Name.fromString(domain + ".");
        SOARecord soaRecord = new SOARecord(zone, DClass.IN, ttl, Name.fromString("ns1.example.com."), Name.fromString("hostmaster.example.com."), 1L, 3600L, 600L, 86400L, minimum);
        response.addRecord(soaRecord, Section.AUTHORITY);
        return response;
    }
}
