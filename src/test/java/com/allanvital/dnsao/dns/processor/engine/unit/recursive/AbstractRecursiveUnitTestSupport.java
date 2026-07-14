package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.unit.RecursiveUnit;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.graph.OverrideRegistry;
import com.allanvital.dnsao.graph.RecursiveInfraAssembler;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class AbstractRecursiveUnitTestSupport {

    protected OverrideRegistry overrideRegistry;
    protected RecursiveInfraAssembler recursiveInfraAssembler;

    protected void resetRecursiveInfra() {
        this.overrideRegistry = new OverrideRegistry();
        this.recursiveInfraAssembler = new RecursiveInfraAssembler(overrideRegistry);
    }

    protected void registerRecursiveOverride(Object instance) {
        this.overrideRegistry.registerOverride(instance);
    }

    protected RecursiveUnit buildRecursiveUnit() {
        return recursiveInfraAssembler.recursiveUnit();
    }

    protected RecursiveUnit buildRecursiveUnit(Object... overrides) {
        for (Object override : overrides) {
            registerRecursiveOverride(override);
        }
        return buildRecursiveUnit();
    }

    protected DnsQueryRequest buildRequest(String domain) throws Exception {
        Message message = MessageHelper.buildARequest(domain);
        DnsQueryRequest request = new DnsQueryRequest(InetAddress.getLoopbackAddress());
        request.setOriginalRequest(message);
        request.setRequest(message);
        request.setIsLocalQuery(false);
        return request;
    }

    protected AuthorityEndpoint endpoint(String name, String ip, int port) throws Exception {
        return new AuthorityEndpoint(name, InetAddress.getByName(ip), port);
    }

    protected void assertCodeResponse(int expected, DnsQueryResponse response) {
        String expectedInfo = Rcode.string(expected) + "(" + expected + ")";
        int responseRcode = response.getResponse().getRcode();
        String actualInfo = Rcode.string(responseRcode) + "(" + responseRcode + ")";

        assertEquals(expected, responseRcode, "Expected %s but got %s".formatted(expectedInfo, actualInfo));
    }

    protected String d(String domain) {
        return domain + ".";
    }

}
