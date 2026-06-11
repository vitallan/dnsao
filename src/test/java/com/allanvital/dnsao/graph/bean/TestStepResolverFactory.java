package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.recursive.StepResolver;
import com.allanvital.dnsao.dns.recursive.StepResolverFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.allanvital.dnsao.Constants.DEFAULT_DNS_PORT;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestStepResolverFactory extends StepResolverFactory {

    private static final String LOOPBACK_IP = "127.0.0.1";

    private int portToUse = DEFAULT_DNS_PORT;
    private final Map<String, Integer> routePortByIp = new ConcurrentHashMap<>();

    public TestStepResolverFactory(int timeoutMs) {
        super(timeoutMs);
    }

    public void setPortToUse(int port) {
        this.portToUse = port;
    }

    public int getPortToUse() {
        return portToUse;
    }

    public void setRoute(String ip, int port) {
        routePortByIp.put(ip, port);
    }

    public void clearRoutes() {
        routePortByIp.clear();
    }

    @Override
    public StepResolver create(String ip, int port) {
        Integer routedPort = routePortByIp.get(ip);
        if (routedPort != null) {
            return super.create(LOOPBACK_IP, routedPort);
        }
        return super.create(ip, portToUse);
    }

}
