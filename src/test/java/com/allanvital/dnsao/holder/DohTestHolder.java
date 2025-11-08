package com.allanvital.dnsao.holder;

import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.fake.FakeDohServer;
import org.junit.jupiter.api.Assertions;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DohTestHolder extends TestHolder {

    protected void startFakeServer() throws ConfException {
        if (conf == null) {
            Assertions.fail("load conf before starting server");
        }
        try {
            fakeUpstreamServer = new FakeDohServer();
            fakeUpstreamServer.start();
            fixUpstreamPorts();
        } catch (Exception e) {
            Assertions.fail("failed dealing with fakeServer: " + e.getMessage());
        }
    }

}
