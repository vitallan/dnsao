package com.allanvital.dnsao.holder;

import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.fake.FakeDotServer;
import org.junit.jupiter.api.Assertions;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DotTestHolder extends TestHolder {

    protected void startFakeServer() throws ConfException {
        if (conf == null) {
            Assertions.fail("load conf before starting server");
        }
        try {
            fakeUpstreamServer = new FakeDotServer();
            fakeUpstreamServer.start();
            fixUpstreamPorts();
        } catch (Exception e) {
            Assertions.fail("failed dealing with fakeServer: " + e.getMessage());
        }
    }

}
