package com.allanvital.dnsao.dns.processor.engine;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.unit.EngineUnit;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryEngine {

    private final EngineUnitProvider engineUnitProvider;

    public QueryEngine(EngineUnitProvider engineUnitProvider) {
        this.engineUnitProvider = engineUnitProvider;
    }

    public DnsQueryResponse process(DnsQueryRequest dnsQueryRequest) {
        for (EngineUnit unit : engineUnitProvider.getOrderedEngineUnits()) {
            DnsQueryResponse response = unit.process(dnsQueryRequest);
            if (response != null) {
                //the first valid one is answered to client
                //thats why it is important to validate the ordering of the engine units
                return response;
            }
        }
        return null;
    }

}
